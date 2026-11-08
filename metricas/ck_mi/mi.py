"""Experimental, explicitly specified lexical Halstead MI; not Visual Studio output."""
import argparse
import csv
import hashlib
import json
import math
from pathlib import Path
import subprocess
from datetime import datetime, timezone

SKIP = {"line_comment", "block_comment", "comment"}
NESTED = {"method_declaration", "constructor_declaration", "compact_constructor_declaration",
          "lambda_expression", "class_body", "class_declaration"}


def halstead(body, source):
    """Identifiers/literals are operands; other lexical tokens are operators.

    Delimiters are included as operators, strings are atomic, comments are ignored.
    Nested executable units and local class bodies belong to their own measurements.
    This convention is reproducible but is not interchangeable with other lexers.
    """
    operators, operands = [], []
    def visit(node):
        if node.type in SKIP or (node != body and node.type in NESTED):
            return
        text = source[node.start_byte:node.end_byte].decode("utf-8")
        if node.type.endswith("literal") or node.type in {"identifier", "type_identifier", "true", "false", "null_literal"}:
            operands.append(text)
        elif not node.children:
            if text.strip():
                operators.append(text)
        else:
            for child in node.children:
                visit(child)
    visit(body)
    n1, n2 = len(set(operators)), len(set(operands))
    N1, N2 = len(operators), len(operands)
    vocabulary, length = n1 + n2, N1 + N2
    volume = length * math.log2(vocabulary) if vocabulary > 1 and length else 0.0
    return {"distinct_operators": n1, "distinct_operands": n2,
            "operators": N1, "operands": N2, "halstead_volume": volume}


def index(volume, cc, loc):
    if volume <= 0 or loc <= 0:
        return None, None
    raw = 171 - 5.2 * math.log(volume) - 0.23 * cc - 16.2 * math.log(loc)
    return raw, max(0.0, min(100.0, raw * 100 / 171))


def self_test():
    from tree_sitter import Language, Parser
    import tree_sitter_java
    parser = Parser(Language(tree_sitter_java.language()))
    source = b'class A { int f(int a) { /* ignored */ return a + 1; } }'
    tree = parser.parse(source)
    body = tree.root_node.named_children[0].child_by_field_name("body").named_children[0].child_by_field_name("body")
    stats = halstead(body, source)
    assert stats["distinct_operands"] == 2 and stats["operands"] == 2
    assert stats["distinct_operators"] == 5  # { return + ; }
    assert "ignored" not in str(stats)
    raw, normalized = index(math.e, 1, 1)
    assert abs(raw - (171 - 5.2 - 0.23)) < 1e-9
    assert 0 <= normalized <= 100 and index(0, 1, 1) == (None, None)
    print("SELF-TEST OK: lexical convention, logarithms, scales and undefined inputs")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--repo", default=".")
    ap.add_argument("--vg", help="Directory containing Juan's methods.csv and metadata.json")
    ap.add_argument("--out")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return
    if not args.vg or not args.out:
        ap.error("--vg and --out are required")
    from tree_sitter import Language, Parser
    import tree_sitter_java
    parser = Parser(Language(tree_sitter_java.language()))
    repo, vg, out = Path(args.repo).resolve(), Path(args.vg).resolve(), Path(args.out).resolve()
    if out.exists():
        raise ValueError("Output exists: choose a new directory")
    inputs = json.loads((vg / "metadata.json").read_text(encoding="utf-8"))
    if inputs.get("tool") != "gym-track-vg-loc/1.0":
        raise ValueError("Unsupported CC/LOC input convention")
    cache, results = {}, []
    with (vg / "methods.csv").open(encoding="utf-8", newline="") as stream:
        rows = list(csv.DictReader(stream))
    for row in rows:
        if row["language"] != "java":
            continue
        filename = row["file"]
        path = (repo / filename).resolve()
        path.relative_to(repo / "src/g-t-api/src/main/java")
        if filename not in cache:
            source = path.read_bytes()
            tree = parser.parse(source)
            if tree.root_node.has_error:
                raise ValueError(f"Parser error in {filename}")
            nodes = {}
            def collect(node):
                nodes[(node.start_byte, node.end_byte)] = node
                for child in node.children:
                    collect(child)
            collect(tree.root_node)
            cache[filename] = (source, tree, nodes)
        source, tree, nodes = cache[filename]
        if hashlib.sha256(source).hexdigest() != row["sha256"]:
            raise ValueError(f"Source changed since CC/LOC: {filename}; remeasure all inputs")
        body = nodes[(int(row["body_start_byte"]), int(row["body_end_byte"]))]
        stats = halstead(body, source)
        raw, normalized = index(stats["halstead_volume"], int(row["vg"]), int(row["body_code_lines"]))
        results.append({**{key: row[key] for key in ["file", "owner", "name", "line", "column", "kind", "vg", "body_code_lines", "sha256"]},
                        **stats, "mi_raw": raw, "mi_0_100": normalized,
                        "state": "defined" if raw is not None else "undefined_zero_input"})
    if not results:
        raise ValueError("No Java units in input")
    out.mkdir(parents=True)
    with (out / "mi.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(results[0]))
        writer.writeheader()
        writer.writerows(results)
    meta = {"tool": "gym-track-mi-lexical/1.0", "input_metadata": inputs,
            "git_head": subprocess.check_output(["git", "-C", str(repo), "rev-parse", "HEAD"], text=True).strip(),
            "generated_utc": datetime.now(timezone.utc).isoformat(),
            "input_csv_sha256": hashlib.sha256((vg / "methods.csv").read_bytes()).hexdigest(),
            "script_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            "warning": "Experimental lexical Halstead convention; do not label as Visual Studio MI or apply thresholds from another scale blindly"}
    (out / "metadata.json").write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    print(f"Written mi.csv and metadata.json to {out}; pass the original outputs to the coordinator")


if __name__ == "__main__":
    main()
