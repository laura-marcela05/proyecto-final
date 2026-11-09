"""Compara salidas ya ejecutadas para un servicio; no ejecuta medidores."""
import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess

AREAS = {"users": "UserService", "students": "StudentService", "sessions": "SessionService"}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def csv_read(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        return list(csv.DictReader(stream))


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def value(text):
    if text is None or text == "":
        return "Sin dato"
    try:
        n = float(text)
        if not __import__('math').isfinite(n):
            return "Indefinido"
        return str(int(n)) if n.is_integer() else f"{n:.4f}"
    except ValueError:
        return str(text)


def delta(before, after):
    if before is None:
        return "Unidad nueva; no comparable"
    if after is None:
        return "Unidad retirada; no comparable"
    try:
        return value(float(after) - float(before))
    except (ValueError, TypeError):
        return "No comparable"


def test_reports(directory):
    result = {}
    for path in sorted(directory.glob("*.txt")):
        match = re.search(r"Tests run:\s*(\d+),\s*Failures:\s*(\d+),\s*Errors:\s*(\d+),\s*Skipped:\s*(\d+)",
                          path.read_text(encoding="utf-8-sig"))
        if not match:
            raise ValueError(f"Resumen Surefire no reconocido: {path}")
        numbers = [int(x) for x in match.groups()]
        if numbers[0] == 0 or any(numbers[1:]):
            raise ValueError(f"Pruebas no satisfactorias: {path}")
        result[path.name] = {"tests": numbers[0], "sha256": sha(path)}
    if not result:
        raise ValueError(f"Faltan resumenes de pruebas: {directory}")
    return result


def named(rows, filename):
    result = {}
    for row in rows:
        if row["file"] != filename or row["kind"] != "method_declaration":
            continue
        # Estos servicios no tienen sobrecarga; si cambia, exigir nueva identidad.
        name = row["name"]
        if name in result:
            raise ValueError("Metodos sobrecargados: revisar identificacion antes de comparar")
        result[name] = row
    return result


def make_rows(filename, klass, old, new):
    rows = []
    def add(element, metric, before, after, note):
        rows.append((element, metric, value(before), value(after), delta(before, after), note))
    for field, label in (("physical_lines", "LOC fisicas archivo"), ("code_lines", "LOC codigo archivo")):
        b = next(r for r in old['files'] if r['file'] == filename)
        a = next(r for r in new['files'] if r['file'] == filename)
        add(klass, label, b[field], a[field], "Tamano del archivo, incluye auxiliares")
    for field in ("wmc", "dit", "noc", "cbo", "rfc", "lcom", "lcom*"):
        b = next(r for r in old['ck'] if r['class'].split('.')[-1] == klass)
        a = next(r for r in new['ck'] if r['class'].split('.')[-1] == klass)
        add(klass, field.upper(), b[field], a[field], "No asumir que extraer un metodo reduce esta metrica")
    b_methods, a_methods = named(old['methods'], filename), named(new['methods'], filename)
    b_mi, a_mi = named(old['mi'], filename), named(new['mi'], filename)
    for name in sorted(b_methods.keys() | a_methods.keys()):
        b, a = b_methods.get(name, {}), a_methods.get(name, {})
        for field, label in (("vg", "V(G)"), ("body_code_lines", "LOC cuerpo")):
            add(klass + '.' + name, label, b.get(field), a.get(field),
                "Comparar tambien auxiliares y clase; menor valor local no prueba mejora global")
        for field, label in (("mi_raw", "MI bruto lexical"), ("mi_0_100", "MI normalizado lexical")):
            add(klass + '.' + name, label, b_mi.get(name, {}).get(field), a_mi.get(name, {}).get(field),
                "Misma convencion lexical; no mezclar escalas")
    for label, data_key in (("Unidades ejecutables del archivo", None), ("Suma V(G) de unidades del archivo", 'vg')):
        selected = [[r for r in dataset['methods'] if r['file'] == filename] for dataset in (old, new)]
        counts = [len(s) if data_key is None else sum(int(r[data_key]) for r in s) for s in selected]
        add(klass, label, *counts, "Incluye constructores y lambdas; esta suma NO es WMC de CK")
    return rows


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--area', choices=AREAS)
    parser.add_argument('--self-test', action='store_true')
    args = parser.parse_args()
    if args.self_test:
        assert delta(None, 1) == "Unidad nueva; no comparable"
        assert delta(3, 2) == '-1'
        assert value('NaN') == 'Indefinido'
        assert named([{'file':'x','name':'f','kind':'method_declaration'}], 'x')['f']['name'] == 'f'
        print('SELF-TEST OK: unidades nuevas, diferencias, indefinidos y metodos')
        return
    if not args.area:
        parser.error('--area es obligatorio')
    repo = Path.cwd()
    before = repo / 'docs/metricas/resultados/baseline'
    after = repo / ('docs/metricas/resultados/refactor-' + args.area)
    out = repo / ('docs/metricas/refactors/' + args.area)
    if out.exists():
        raise ValueError('Comparacion ya existe: no sobrescribir evidencia')
    klass = AREAS[args.area]
    filename = 'src/g-t-api/src/main/java/com/corhuila/gymtracker/service/' + klass + '.java'
    datasets, input_hashes, revisions, conventions = [], {}, [], []
    for label, directory in (('antes', before), ('despues', after)):
        files = {'files': 'vg_loc/files.csv', 'methods': 'vg_loc/methods.csv', 'ck': 'ck/ck.csv', 'mi': 'mi/mi.csv'}
        dataset = {key: csv_read(directory / relative) for key, relative in files.items()}
        vg_meta = read_json(directory / 'vg_loc/metadata.json')
        ck_meta = read_json(directory / 'ck/metadata.json')
        mi_meta = read_json(directory / 'mi/metadata.json')
        if len({m['git_head'] for m in (vg_meta, ck_meta, mi_meta)}) != 1:
            raise ValueError('Revision distinta dentro de la misma corrida')
        if mi_meta['input_csv_sha256'] != sha(directory / 'vg_loc/methods.csv'):
            raise ValueError('El MI no corresponde a methods.csv de su corrida')
        if mi_meta['input_metadata']['git_head'] != vg_meta['git_head']:
            raise ValueError('MI identifica otra entrada de V(G)')
        conventions.append((vg_meta['tool'], vg_meta['versions'], ck_meta['version'],
                            ck_meta['jar_sha256'], mi_meta['tool']))
        for relative in [*files.values(), 'vg_loc/metadata.json', 'ck/metadata.json', 'mi/metadata.json', 'ck/class.csv', 'ck/method.csv', 'ck/ck.log']:
            input_hashes[label + '/' + relative] = sha(directory / relative)
        if 'Error in ' in (directory / 'ck/ck.log').read_text(encoding='utf-8-sig'):
            raise ValueError('CK informo error de analisis')
        revisions.append(vg_meta['git_head'])
        datasets.append(dataset)
    if conventions[0] != conventions[1]:
        raise ValueError('Cambiaron herramientas o convenciones: no comparar sin revisar')
    old_by_file = {r['file']: r['sha256'] for r in datasets[0]['files']}
    new_by_file = {r['file']: r['sha256'] for r in datasets[1]['files']}
    if old_by_file.keys() != new_by_file.keys():
        raise ValueError('Cambio el alcance de produccion; revisar comparabilidad')
    changed = {name for name in old_by_file if old_by_file[name] != new_by_file[name]}
    if changed != {filename}:
        raise ValueError(f'Esta comparacion individual espera solo {filename}; encontro {sorted(changed)}')
    if sha(repo / filename) != new_by_file[filename]:
        raise ValueError('El servicio cambio despues de la medicion')
    tests = {label: test_reports(after / ('pruebas-' + label)) for label in ('antes', 'despues')}
    counts = [{k: v['tests'] for k, v in reports.items()} for reports in tests.values()]
    if counts[0] != counts[1]:
        raise ValueError('Las suites antes/despues no coinciden; revisar antes de afirmar equivalencia')
    expected = 'com.corhuila.gymtracker.service.' + klass + 'Test.txt'
    if expected not in tests['antes']:
        raise ValueError('Falta la prueba del servicio afectado')
    rows = make_rows(filename, klass, *datasets)
    out.mkdir(parents=True)
    with (out / 'comparacion.csv').open('w', encoding='utf-8', newline='') as stream:
        writer = csv.writer(stream)
        writer.writerow(['elemento','metrica','antes','despues','cambio','lectura'])
        writer.writerows(rows)
    md = [f'# Antes y despues - {args.area}',
          f'Antes: `{revisions[0]}`. Despues medido: `{revisions[1]}`.',
          'Las pruebas reforzadas se ejecutaron antes y despues del cambio de produccion. '
          'La igualdad de resultados respalda los casos cubiertos; no demuestra equivalencia para todas las entradas.',
          '| Elemento | Metrica | Antes | Despues | Cambio | Lectura |',
          '|---|---|---:|---:|---|---|']
    md.extend('| ' + ' | '.join(str(v).replace('|', '\\|') for v in row) + ' |' for row in rows)
    md.extend(['\n## Interpretacion',
               'La extraccion agrupa una regla repetida y conserva el orden observable. '
               'No se exige reducir todos los indicadores. Revisar metodos originales, auxiliar nuevo, '
               'LOC del archivo y CK de clase: si WMC o LOC aumentan se informa explicitamente.',
               'Las filas nuevas no tienen un antes numerico comparable; no se inventa cero. '
               'MI mantiene la misma convencion experimental y sus escalas separadas. '
               'Las lambdas y constructores se incluyen en el conteo y suma de unidades del archivo, '
               'pero no se alinean individualmente por numeros de linea que cambiaron.',
               '\n## Limites',
               'Corrida individual: solo cambia el servicio de esta persona respecto a baseline. '
               'Las otras ramas de refactor se integran por separado. Tras los tres merges se realizara '
               'una verificacion conjunta; no presentar estas tres corridas como una corrida unica del main final.',
               'Fuentes: salidas originales de baseline y refactor-' + args.area + ', fichas del equipo y resumenes Surefire conservados.'])
    (out / 'comparacion.md').write_text('\n\n'.join(md) + '\n', encoding='utf-8')
    meta = {'tool':'gym-track-refactor-compare/1.0', 'area':args.area, 'class':klass,
            'baseline_revision':revisions[0], 'after_revision':revisions[1],
            'generated_utc':datetime.now(timezone.utc).isoformat(),
            'git_head':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),
            'script_sha256':sha(Path(__file__)), 'input_sha256':input_hashes, 'tests':tests,
            'output_sha256':{p.name:sha(p) for p in out.iterdir() if p.is_file()}}
    (out / 'metadata.json').write_text(json.dumps(meta,indent=2) + '\n',encoding='utf-8')
    print('COMPARACION OK: comparacion.csv, comparacion.md y metadata.json')
    print('Leer cambios y enviar salidas al coordinador antes de aprobar el PR')


if __name__ == '__main__':
    main()
