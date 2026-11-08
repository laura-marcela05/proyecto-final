"""Consolida salidas existentes; no mide fuentes ni modifica la linea base."""
import argparse
import csv
from datetime import datetime, timezone
from decimal import Decimal
import hashlib
import importlib.metadata
import json
import math
from pathlib import Path
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read_csv(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def num(value):
    result = float(value)
    if not math.isfinite(result):
        raise ValueError("Numero no finito: revisar la salida original")
    return result


def fmt(value):
    result = num(value)
    return str(int(result)) if result.is_integer() else f"{result:.2f}"


def unit(row):
    owner = row.get("owner", "")
    name = (owner + "." if owner else "") + row["name"]
    return f"{row['file']}:{row['line']}:{row['column']} ({name})"


def key(row):
    return tuple(row[k] for k in ("file", "owner", "name", "line", "column", "kind"))


def vg_action(value):
    v = num(value)
    if v <= 10:
        return "Bajo; mantener pruebas y revisar cambios futuros"
    if v <= 20:
        return "Moderado; revisar decisiones y posible Extract Method"
    if v <= 50:
        return "Alto; priorizar revision con pruebas de regresion"
    return "Muy alto; revisar diseno antes de ampliar comportamiento"


def ck_action(metric, value):
    if metric in {"wmc", "dit", "cbo", "rfc"}:
        limit = {"wmc": 20, "dit": 5, "cbo": 10, "rfc": 50}[metric]
        return (f"<= {limit}", "Alerta orientativa; revisar responsabilidades y dependencias"
                if num(value) > limit else "Dentro de referencia; conservar pruebas")
    if metric == "lcom*":
        try:
            v = num(value)
        except ValueError:
            return "<= 0.7 orientativo", "Indefinido; revisar caso degenerado, no convertir en cero"
        if not 0 <= v <= 1:
            return "<= 0.7 orientativo", "Fuera de escala; revision manual"
        return "<= 0.7 orientativo", ("Alerta de cohesion; revisar entidad/anotaciones antes de refactorizar"
                                           if v > .7 else "Dentro de referencia orientativa")
    return "Sin umbral universal", "Descriptivo; no interpretar LCOM bruto con umbral 0.7"


def escape(value):
    return str(value).replace("|", "\\|").replace("\n", " ").replace("\r", " ")


def table(rows):
    result = ["| Elemento medido | Metrica | Valor obtenido | Umbral | Estado / Accion propuesta |",
              "|---|---|---:|---|---|"]
    result.extend("| " + " | ".join(escape(v) for v in row) + " |" for row in rows)
    return "\n".join(result)


def load_inputs(repo, baseline):
    names = ["vg_loc/files.csv", "vg_loc/methods.csv", "vg_loc/metadata.json",
             "ck/ck.csv", "ck/class.csv", "ck/method.csv", "ck/ck.log", "ck/metadata.json",
             "mi/mi.csv", "mi/metadata.json", "issues.json", "mantenimiento/maintenance.json"]
    paths = {name: baseline / name for name in names}
    paths["registro.csv"] = repo / "metricas/mantenimiento/registro.csv"
    for path in paths.values():
        if not path.is_file():
            raise ValueError(f"Falta entrada: {path}; actualiza main antes de crear la rama")
    json_read = lambda name: json.loads(paths[name].read_text(encoding="utf-8-sig"))
    metas = [json_read("vg_loc/metadata.json"), json_read("ck/metadata.json"),
             json_read("mi/metadata.json"), json_read("mantenimiento/maintenance.json")["metadata"]]
    heads = {m["git_head"] for m in metas}
    if len(heads) != 1 or metas[2]["input_metadata"]["git_head"] not in heads:
        raise ValueError("Las mediciones no identifican la misma revision; revisar, no mezclar")
    if metas[2]["input_csv_sha256"] != sha(paths["vg_loc/methods.csv"]):
        raise ValueError("MI no corresponde al methods.csv recibido")
    if metas[3]["records_sha256"] != sha(paths["registro.csv"]):
        raise ValueError("Cambio registro.csv desde la medicion; revisar procedencia")
    if metas[3]["issues_sha256"] != sha(paths["issues.json"]):
        raise ValueError("Cambio snapshot de issues desde la medicion")
    files, methods = read_csv(paths["vg_loc/files.csv"]), read_csv(paths["vg_loc/methods.csv"])
    ck, mi = read_csv(paths["ck/ck.csv"]), read_csv(paths["mi/mi.csv"])
    if not all([files, methods, ck, mi]):
        raise ValueError("Alguna salida esta vacia")
    by_file = {r["file"]: r for r in files}
    if len(by_file) != len(files):
        raise ValueError("Archivos duplicados en files.csv")
    by_method = {key(r): r for r in methods}
    if len(by_method) != len(methods):
        raise ValueError("Unidades duplicadas en methods.csv")
    for row in files:
        if int(row["physical_lines"]) != sum(int(row[k]) for k in ("code_lines", "comment_only_lines", "blank_lines")):
            raise ValueError("LOC no reconcilia")
        if sha(repo / row["file"]) != row["sha256"]:
            raise ValueError(f"Fuentes cambiaron desde baseline: {row['file']}")
    for row in methods:
        if row["sha256"] != by_file[row["file"]]["sha256"] or int(row["vg"]) < 1:
            raise ValueError("Hash o V(G) inconsistente")
    for row in mi:
        original = by_method[key(row)]
        for field in ("vg", "body_code_lines", "sha256"):
            if row[field] != original[field]:
                raise ValueError(f"MI mezcla unidades incompatibles: {unit(row)}")
    if len(mi) != sum(r["language"] == "java" for r in methods):
        raise ValueError("Cobertura MI distinta de las unidades Java")
    for relative, digest in metas[1]["source_sha256"].items():
        if sha(repo / relative) != digest:
            raise ValueError(f"Fuente CK modificada: {relative}")
    maintenance = json_read("mantenimiento/maintenance.json")
    records = read_csv(paths["registro.csv"])
    if not records or sum(Decimal(r["hours"]) for r in records) != Decimal(maintenance["hours_total"]):
        raise ValueError("Esfuerzo no reconcilia con registro.csv")
    if sum(Decimal(r["hours"]) for r in maintenance["types"]) != Decimal(maintenance["hours_total"]):
        raise ValueError("Totales por tipo no reconcilian")
    if "Error in " in paths["ck/ck.log"].read_text(encoding="utf-8-sig"):
        raise ValueError("CK registro un error de analisis")
    return files, methods, ck, mi, maintenance, paths, next(iter(heads))


def build_tables(files, methods, ck, mi, maintenance):
    vg_rows, loc_rows, ck_rows, mi_rows, effort_rows = [], [], [], [], []
    for r in sorted(methods, key=lambda r: (-int(r["vg"]), r["file"], int(r["line"]), int(r["column"]))):
        vg_rows.append((unit(r), "V(G)", r["vg"], "1-10 bajo; 11-20 moderado; 21-50 alto; >50 muy alto", vg_action(r["vg"])))
        loc_rows.append((unit(r), "LOC del cuerpo", r["body_code_lines"], "<= 50 orientativo",
                         "Revisar longitud" if int(r["body_code_lines"]) > 50 else "Dentro de referencia; incluye llaves y declaraciones"))
    for lang in ("java", "typescript"):
        selected = [r for r in files if r["language"] == lang]
        for field, label in (("physical_lines", "LOC fisicas"), ("code_lines", "LOC codigo"),
                             ("comment_only_lines", "LOC solo comentario"), ("blank_lines", "LOC blanco")):
            loc_rows.append((lang, label, sum(int(r[field]) for r in selected), "Sin umbral universal", "Tamano del alcance; no representa todo el repositorio"))
    for r in sorted(ck, key=lambda r: r["class"]):
        for metric in ("wmc", "dit", "noc", "cbo", "rfc", "lcom", "lcom*"):
            threshold, action = ck_action(metric, r[metric])
            ck_rows.append((r["class"], metric.upper(), r[metric], threshold, action))
    for r in sorted(mi, key=lambda r: (num(r["mi_raw"]) if r["mi_raw"] else math.inf, r["file"], int(r["line"]))):
        defined = r["state"] == "defined" and bool(r["mi_raw"])
        raw_action = ("Alerta orientativa; investigar CC, LOC y volumen lexical" if defined and num(r["mi_raw"]) < 65
                      else "Sin alerta respecto a esta referencia; lexer propio no calibrado") if defined else "Indefinido: no sustituir por cero"
        mi_rows.append((unit(r), "MI bruto lexical", fmt(r["mi_raw"]) if defined else "Sin dato", ">= 65 orientativo del curso", raw_action))
        mi_rows.append((unit(r), "MI normalizado lexical", fmt(r["mi_0_100"]) if defined else "Sin dato", "Sin umbral validado para este lexer", "Solo comparacion interna; no aplicar 65 a escala 0-100"))
    for r in maintenance["types"]:
        effort_rows.append((r["type"], "Horas registradas", r["hours"], "Sin umbral universal", "Estimaciones retrospectivas; no es tiempo cronometrado" if maintenance["has_estimates"] else "Solo esfuerzo registrado"))
        effort_rows.append((r["type"], "% del esfuerzo registrado", fmt(r["effort_percent"]), "Sin meta porcentual obligatoria", "Sin registros de este tipo" if not r["records"] else "Distribucion del registro; no extrapolar a todo el proyecto"))
    effort_rows.append(("Total registrado", "Horas", maintenance["hours_total"], "Sin umbral universal", "No incluye importacion, esperas ni todo el trabajo academico"))
    effort_rows.append(("Total registrado", "Costo monetario", maintenance["cost_total"] if maintenance["cost_total"] is not None else "Sin dato", "Requiere tarifa y moneda documentadas", "No inventar una tarifa ni interpretar null como cero"))
    return {"V(G) por unidad": vg_rows, "LOC por unidad y lenguaje": loc_rows,
            "Suite CK por clase": ck_rows, "MI por unidad Java": mi_rows,
            "Esfuerzo de mantenimiento registrado": effort_rows}


def charts(out, files, methods, ck, mi, maintenance):
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    plt.rcParams.update({"font.size": 10, "axes.spines.top": False, "axes.spines.right": False})

    def horizontal(filename, title, labels, values, xlabel, limit=None):
        fig, ax = plt.subplots(figsize=(12, max(4.5, .44 * len(values) + 1.5)), layout="constrained")
        bars = ax.barh(range(len(values)), values, color="#27648a")
        ax.set_yticks(range(len(values)), labels=labels)
        ax.invert_yaxis()
        ax.set_xlabel(xlabel)
        ax.set_title(title, loc="left", pad=14)
        low = min(min(values, default=0), 0)
        ax.set_xlim(low * 1.2, max(max(values, default=0), limit or 0, 1) * 1.2)
        ax.bar_label(bars, labels=[fmt(v) for v in values], padding=4)
        if limit is not None:
            ax.axvline(limit, color="#b14a22", linestyle="--", label=f"Referencia {limit}")
            ax.legend(loc="lower right")
        ax.xaxis.grid(True, alpha=.15)
        ax.set_axisbelow(True)
        fig.savefig(out / filename, dpi=160, facecolor="white")
        plt.close(fig)

    top = sorted(methods, key=lambda r: (-int(r["vg"]), r["file"], int(r["line"])))[:10]
    labels = [f"{Path(r['file']).name}:{r['line']}:{r['column']} {r.get('owner', '')}.{r['name']}" for r in top]
    horizontal("01-vg.png", "V(G): diez unidades con mayor valor", labels, [int(r["vg"]) for r in top], "Complejidad por unidad", 10)
    langs = ("java", "typescript")
    fig, ax = plt.subplots(figsize=(9, 5), layout="constrained")
    bottoms = [0, 0]
    for field, label, color in (("code_lines", "Codigo", "#27648a"), ("comment_only_lines", "Solo comentario", "#c3a23a"), ("blank_lines", "Blanco", "#bcc8d0")):
        values = [sum(int(r[field]) for r in files if r["language"] == lang) for lang in langs]
        ax.bar(langs, values, bottom=bottoms, label=label, color=color)
        bottoms = [a + b for a, b in zip(bottoms, values)]
    ax.set_title("LOC fisicas del alcance por lenguaje", loc="left")
    ax.set_ylabel("Lineas fisicas")
    ax.legend()
    fig.savefig(out / "02-loc.png", dpi=160, facecolor="white")
    plt.close(fig)
    for field, filename, title, limit in (("cbo", "03-cbo.png", "CK: acoplamiento por clase (diez mayores)", 10),
                                          ("wmc", "04-wmc.png", "CK: WMC por clase (diez mayores)", 20)):
        selected = sorted(ck, key=lambda r: (-num(r[field]), r["class"]))[:10]
        horizontal(filename, title, [r["class"].split(".")[-1] for r in selected], [num(r[field]) for r in selected], field.upper(), limit)
    selected = sorted([r for r in mi if r["state"] == "defined" and r["mi_raw"]], key=lambda r: (num(r["mi_raw"]), r["file"], int(r["line"])))[:10]
    if not selected:
        raise ValueError("No hay unidades MI definidas; revisar antes de graficar")
    horizontal("05-mi-bruto.png", "MI bruto lexical: diez valores menores (referencia orientativa)",
               [f"{Path(r['file']).name}:{r['line']}:{r['column']} {r['name']}" for r in selected],
               [num(r["mi_raw"]) for r in selected], "MI bruto (no escala 0-100)", 65)
    horizontal("06-esfuerzo.png", "Esfuerzo registrado por tipo: estimacion retrospectiva" if maintenance["has_estimates"] else "Esfuerzo registrado por tipo",
               [r["type"] for r in maintenance["types"]], [num(r["hours"]) for r in maintenance["types"]], "Horas registradas; tipos sin registros aparecen en cero")


def self_test():
    assert "Bajo" in vg_action(10) and "Moderado" in vg_action(11)
    assert "Dentro" in ck_action("wmc", 20)[1] and "Alerta" in ck_action("wmc", 21)[1]
    assert "degenerado" in ck_action("lcom*", "NaN")[1]
    assert "Fuera" in ck_action("lcom*", "1.5")[1]
    assert "0.7" not in ck_action("lcom", 2)[0]
    assert "\\|" in table([("a|b", "metric", 2, "none", "review")])
    assert fmt("0.17") == "0.17"
    # Solo datos sinteticos en memoria, nunca resultados del equipo.
    method = {"file": "fixture.java", "owner": "A", "name": "f", "line": "1",
              "column": "1", "kind": "method_declaration", "vg": "1", "body_code_lines": "1"}
    source = {"file": "fixture.java", "language": "java", "physical_lines": "1",
              "code_lines": "1", "comment_only_lines": "0", "blank_lines": "0"}
    klass = {"class": "A", **{m: "1" for m in ("wmc", "dit", "noc", "cbo", "rfc", "lcom")}, "lcom*": "0"}
    index = {**method, "state": "undefined_zero_input", "mi_raw": "", "mi_0_100": ""}
    effort = {"types": [{"type": "preventivo", "hours": "1", "effort_percent": "100", "records": 1}],
              "has_estimates": True, "hours_total": "1", "cost_total": None}
    result = build_tables([source], [method], [klass], [index], effort)
    assert len(result["Suite CK por clase"]) == 7
    assert all(r[2] == "Sin dato" for r in result["MI por unidad Java"])
    assert result["Esfuerzo de mantenimiento registrado"][-1][2] == "Sin dato"
    print("SELF-TEST OK: umbrales, limites CK, indefinidos y tabla Anexo C")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--baseline", default="docs/metricas/resultados/baseline")
    parser.add_argument("--out", default="docs/metricas/anexo-c")
    args = parser.parse_args()
    if args.self_test:
        self_test()
        return
    repo, baseline, out = Path.cwd(), Path(args.baseline).resolve(), Path(args.out).resolve()
    if out.exists():
        raise ValueError("La salida ya existe: conservarla y consultar antes de repetir")
    files, methods, ck, mi, maintenance, inputs, revision = load_inputs(repo, baseline)
    # Comprobar dependencia antes de crear archivos parciales.
    import matplotlib
    sections = build_tables(files, methods, ck, mi, maintenance)
    body = ["# Anexo C - resultados iniciales", f"\nRevision medida: `{revision}`.",
            "\nFuente: salidas originales en ../resultados/baseline; fichas y umbrales en ../fichas.",
            "\nGenerado sin volver a medir ni modificar fuentes. Cada tabla conserva las cinco columnas del Anexo C de la guia.",
            "\n## Alcance y limites",
            "\nV(G)/LOC: Java y TypeScript de produccion del alcance declarado. LOC del cuerpo incluye llaves y declaraciones; no sumar cuerpos anidados para obtener LOC del proyecto.",
            "\nCK: clases publicas de service/security/controller/entity; sin resolver JAR externos. CBO cuenta dependencias de frameworks: alerta no equivale a defecto. WMC de CK no se sustituye por suma de V(G).",
            "\nLCOM bruto y LCOM* son distintos. LCOM* fuera de escala o indefinido requiere revision manual; una entidad con getters/setters no se divide automaticamente por su LCOM*.",
            "\nMI: lexer propio experimental, solo Java. El umbral 65 se muestra en escala bruta, con cautela; no se aplica a escala normalizada ni se afirma equivalencia con Visual Studio.",
            "\nMantenimiento: solo esfuerzo registrado; los tipos sin filas no demuestran ausencia de ese trabajo. Las estimaciones deben haber sido validadas por cada integrante. Costo null significa sin dato, no cero."]
    for title, rows in sections.items():
        body.extend(["\n## " + title, "\n" + table(rows)])
    body.extend(["\n## Graficos",
                 "\n" + "\n\n".join(f"![{label}]({filename})" for filename, label in (
                     ("01-vg.png", "V(G) mayor"), ("02-loc.png", "LOC por lenguaje"),
                     ("03-cbo.png", "CBO por clase"), ("04-wmc.png", "WMC por clase"),
                     ("05-mi-bruto.png", "MI bruto menor"), ("06-esfuerzo.png", "Esfuerzo registrado"))),
                 "\n## Antes y despues",
                 "\nPendiente de la tarea de refactorizacion. No existe aun una medicion posterior; no repetir baseline como si fuera despues.",
                 "\n## Referencias",
                 "\nGuia del docente, Anexo C; fichas de metricas del equipo; https://matplotlib.org/3.10.8/ ; https://learn.microsoft.com/en-us/visualstudio/code-quality/code-metrics-maintainability-index-range-and-meaning"])
    out.mkdir(parents=True)
    charts(out, files, methods, ck, mi, maintenance)
    (out / "resultados.md").write_text("\n\n".join(body) + "\n", encoding="utf-8")
    metadata = {"tool": "gym-track-anexo-c/1.0", "baseline_revision": revision,
                "generated_utc": datetime.now(timezone.utc).isoformat(),
                "git_head": subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip(),
                "input_sha256": {name: sha(path) for name, path in inputs.items()},
                "script_sha256": sha(Path(__file__)), "matplotlib_version": matplotlib.__version__,
                "table_rows": {name: len(rows) for name, rows in sections.items()},
                "result_sha256": {p.name: sha(p) for p in sorted(out.iterdir()) if p.is_file()}}
    (out / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print("ANEXO C OK: resultados.md, seis PNG y metadata.json")
    print("Abrir las seis imagenes y revisar tablas antes del commit; no se ha interpretado todo automaticamente")


if __name__ == "__main__":
    main()
