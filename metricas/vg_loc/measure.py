"""Reproducible source-level V(G)/physical LOC; see the measurement sheets."""
import argparse
import csv
import hashlib
import importlib.metadata
import json
from pathlib import Path
import subprocess
from datetime import datetime, timezone

FUNCTIONS = {"method_declaration", "constructor_declaration", "compact_constructor_declaration",
             "lambda_expression", "function_declaration", "function_expression",
             "arrow_function", "method_definition", "generator_function_declaration"}
DECISIONS = {"if_statement", "for_statement", "enhanced_for_statement", "for_in_statement",
             "while_statement", "do_statement", "catch_clause", "ternary_expression"}
COMMENTS = {"line_comment", "block_comment", "comment"}
SCOPES = [("src/g-t-api/src/main/java", ".java", "java"),
          ("src/g-t-portal/src", ".ts", "typescript")]


def parser(language):
    from tree_sitter import Language, Parser
    if language == "java":
        import tree_sitter_java
        grammar = tree_sitter_java.language()
    else:
        import tree_sitter_typescript
        grammar = tree_sitter_typescript.language_typescript()
    return Parser(Language(grammar))


def walk(node):
    yield node
    for child in node.children:
        yield from walk(child)


def line_counts(data, tree):
    """Disjoint categories; a mixed comment/code line is a code line."""
    stripped = bytearray(data)
    comment_rows = set()
    for node in walk(tree):
        if node.type not in COMMENTS:
            continue
        end = node.end_point.row + (1 if node.end_point.column else 0)
        comment_rows.update(range(node.start_point.row, end))
        for i in range(node.start_byte, node.end_byte):
            if stripped[i] not in (10, 13):
                stripped[i] = 32
    lines = data.splitlines()
    clean = bytes(stripped).splitlines()
    code = sum(bool(line.strip()) for line in clean)
    comment_only = sum(not clean[i].strip() and i in comment_rows for i in range(len(lines)))
    blank = len(lines) - code - comment_only
    return {"physical_lines": len(lines), "code_lines": code,
            "comment_only_lines": comment_only, "blank_lines": blank,
            "comment_percent": round(100 * comment_only / len(lines), 6) if lines else 0}


def complexity(body, data):
    """Count syntactic decisions; nested executable units get their own rows."""
    value = 1
    def visit(node):
        nonlocal value
        if node != body and (node.type in FUNCTIONS or node.type in {"class_body", "class_declaration"}):
            return
        if node.type in DECISIONS:
            value += 1
        if node.type in {"switch_label", "switch_case"}:
            # One decision per non-default label, including Java arrow cases.
            if any(child.type == "case" for child in node.children):
                value += 1
        if node.type == "binary_expression":
            operator = node.child_by_field_name("operator")
            if operator and data[operator.start_byte:operator.end_byte] in (b"&&", b"||"):
                value += 1
        for child in node.children:
            visit(child)
    visit(body)
    return value


def analyze(data, language, filename):
    tree = parser(language).parse(data)
    if tree.root_node.has_error:
        raise ValueError(f"Parser error in {filename}; do not publish incomplete measurements")
    stats = line_counts(data, tree.root_node)
    rows = []
    for node in walk(tree.root_node):
        if node.type not in FUNCTIONS:
            continue
        body = node.child_by_field_name("body")
        if body is None:
            # Abstract/interface signatures have no executable body.
            continue
        name_node = node.child_by_field_name("name")
        name = data[name_node.start_byte:name_node.end_byte].decode("utf-8") if name_node else "lambda"
        owner = []
        ancestor = node.parent
        while ancestor:
            if ancestor.type in {"class_declaration", "record_declaration", "enum_declaration"}:
                n = ancestor.child_by_field_name("name")
                if n:
                    owner.insert(0, data[n.start_byte:n.end_byte].decode("utf-8"))
            ancestor = ancestor.parent
        body_data = data[body.start_byte:body.end_byte]
        # A bare method block is not a full Java compilation unit. Mask comments
        # using the original AST and slice bytes instead of reparsing that block.
        masked = bytearray(body_data)
        for descendant in walk(body):
            if descendant.type in COMMENTS:
                for i in range(descendant.start_byte - body.start_byte, descendant.end_byte - body.start_byte):
                    if masked[i] not in (10, 13):
                        masked[i] = 32
        nloc = sum(bool(line.strip()) for line in bytes(masked).splitlines())
        rows.append({"file": filename, "language": language, "owner": ".".join(owner),
                     "name": name, "kind": node.type, "line": node.start_point.row + 1,
                     "column": node.start_point.column + 1, "body_start_byte": body.start_byte,
                     "body_end_byte": body.end_byte, "vg": complexity(body, data),
                     "body_code_lines": nloc, "sha256": hashlib.sha256(data).hexdigest()})
    return stats, rows


def git(repo, *args):
    return subprocess.check_output(["git", "-C", str(repo), *args], text=True).strip()


def sources(repo):
    found = []
    for directory, suffix, language in SCOPES:
        folder = repo / directory
        if not folder.is_dir():
            raise ValueError(f"Missing scope: {directory}")
        for path in sorted(folder.rglob("*" + suffix)):
            if path.name.endswith((".spec.ts", ".d.ts")):
                continue
            relative = path.relative_to(repo).as_posix()
            if not git(repo, "ls-files", "--", relative):
                raise ValueError(f"Untracked source: {relative}")
            found.append((relative, language))
    if git(repo, "status", "--porcelain", "--", *[item[0] for item in SCOPES]):
        raise ValueError("Commit or resolve source changes before measuring")
    return found


def write_csv(path, rows, columns):
    with path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)


def self_test():
    java = b'''class A { int f(boolean a, boolean b) {
      // if while && are comments
      String s = "if &&";
      if (a && b) return 1;
      return 0;
    }}'''
    counts, rows = analyze(java, "java", "fixture.java")
    assert rows[0]["vg"] == 3
    assert counts["physical_lines"] == counts["code_lines"] + counts["comment_only_lines"] + counts["blank_lines"]
    record = b'record R(int x) { R { if (x < 0) throw new IllegalArgumentException(); } }'
    assert analyze(record, "java", "R.java")[1][0]["vg"] == 2
    ts = b'const f = (x: boolean) => x ? 1 : 0;'
    assert analyze(ts, "typescript", "f.ts")[1][0]["vg"] == 2
    switch = b'class S { int f(int x) { return switch(x) { case 1 -> 1; default -> 0; }; } }'
    assert analyze(switch, "java", "S.java")[1][0]["vg"] == 2
    nested = b'class N { int f(boolean x) { java.util.function.IntSupplier s = () -> x ? 1 : 0; return s.getAsInt(); } }'
    assert [r["vg"] for r in analyze(nested, "java", "N.java")[1]] == [1, 2]
    print("SELF-TEST OK: Java, records, switch, TypeScript, nested lambdas and comments")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--repo", default=".")
    ap.add_argument("--out")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return
    if not args.out:
        ap.error("--out is required; use a new output directory")
    repo = Path(args.repo).resolve()
    out = Path(args.out).resolve()
    if out.exists():
        raise ValueError("Output exists; choose a new directory to preserve evidence")
    file_rows, method_rows = [], []
    for filename, language in sources(repo):
        data = (repo / filename).read_bytes()
        counts, methods = analyze(data, language, filename)
        file_rows.append({"file": filename, "language": language, **counts,
                          "sha256": hashlib.sha256(data).hexdigest()})
        method_rows.extend(methods)
    if not method_rows:
        raise ValueError("No executable units detected")
    out.mkdir(parents=True)
    write_csv(out / "files.csv", file_rows, list(file_rows[0]))
    write_csv(out / "methods.csv", method_rows, list(method_rows[0]))
    meta = {"tool": "gym-track-vg-loc/1.0", "git_head": git(repo, "rev-parse", "HEAD"),
            "generated_utc": datetime.now(timezone.utc).isoformat(), "scope": SCOPES,
            "units": "explicit methods, constructors, lambdas and TypeScript functions; no generated methods",
            "script_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            "versions": {name: importlib.metadata.version(name) for name in
                         ["tree-sitter", "tree-sitter-java", "tree-sitter-typescript"]},
            "file_count": len(file_rows), "unit_count": len(method_rows)}
    (out / "metadata.json").write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    print(f"Written files.csv, methods.csv and metadata.json to {out}")
    print("Pass these files to the coordinator; no results are pre-filled in the report")


if __name__ == "__main__":
    main()
