"""Run CK 0.7.0 with a verified artifact and preserve its original CSV files."""
import argparse
import csv
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.request

VERSION = "0.7.0"
URL = "https://repo.maven.apache.org/maven2/com/github/mauricioaniche/ck/0.7.0/ck-0.7.0-jar-with-dependencies.jar"
SHA256 = "2ddfdc275b6b59c2033e03253c4fec511c338fe494a10b70f651bc039a72c74d"
FIELDS = ["file", "class", "type", "wmc", "dit", "noc", "cbo", "rfc", "lcom", "lcom*"]


def artifact():
    cache = Path(__file__).resolve().parent / ".cache"
    cache.mkdir(exist_ok=True)
    jar = cache / "ck-0.7.0.jar"
    if not jar.exists():
        with urllib.request.urlopen(URL, timeout=90) as response:
            blob = response.read()
        if hashlib.sha256(blob).hexdigest() != SHA256:
            raise ValueError("CK download checksum mismatch")
        jar.write_bytes(blob)
    if hashlib.sha256(jar.read_bytes()).hexdigest() != SHA256:
        raise ValueError("Cached CK checksum mismatch; do not run this artifact")
    return jar


def invoke(jar, source, out):
    out.mkdir(parents=True)
    command = ["java", "-jar", str(jar), str(source), "false", "0", "false", str(out) + "/"]
    result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")
    log = result.stdout + "\n" + result.stderr
    (out / "ck.log").write_text(log, encoding="utf-8")
    if result.returncode or "Error in " in log:
        raise ValueError(f"CK failed or reported parsing errors; inspect {out / 'ck.log'}")
    with (out / "class.csv").open(encoding="utf-8", errors="replace", newline="") as stream:
        reader = csv.DictReader(stream)
        if not set(FIELDS).issubset(reader.fieldnames or []):
            raise ValueError("Unexpected CK CSV schema")
        rows = list(reader)
    if not rows:
        raise ValueError("CK emitted no classes")
    return command, rows


def self_test():
    jar = artifact()
    with tempfile.TemporaryDirectory(dir=jar.parent) as directory:
        fixture = Path(directory) / "source"
        fixture.mkdir()
        (fixture / "A.java").write_text("package sample; public class A { public int f(int x) { if (x > 0) return 1; return 0; } }", encoding="utf-8")
        (fixture / "B.java").write_text("package sample; public class B extends A { }", encoding="utf-8")
        command, rows = invoke(jar, fixture, Path(directory) / "out")
        mapped = {row["class"]: row for row in rows}
        assert {"sample.A", "sample.B"}.issubset(mapped)
        assert int(mapped["sample.A"]["noc"]) == 1
        assert int(mapped["sample.A"]["wmc"]) >= 2
    print("SELF-TEST OK: verified CK artifact, CSV schema and fixture inheritance")


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
        ap.error("--out is required")
    repo, out = Path(args.repo).resolve(), Path(args.out).resolve()
    source = repo / "src/g-t-api/src/main/java"
    if not source.is_dir() or out.exists():
        raise ValueError("Missing Java production scope or output already exists")
    status = subprocess.check_output(["git", "-C", str(repo), "status", "--porcelain", "--", str(source)], text=True)
    if status.strip():
        raise ValueError("Source changes/untracked source detected: resolve before measuring")
    hashes, expected = {}, set()
    for path in sorted(source.rglob("*.java")):
        relative = path.relative_to(repo).as_posix()
        hashes[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
        # CK interpretation is restricted to explicit classes in the principal API layers.
        if path.parent.name in {"service", "security", "controller", "entity"}:
            if re.search(r"\bpublic\s+(?:abstract\s+|final\s+)?class\s+" + re.escape(path.stem) + r"\b", path.read_text(encoding="utf-8")):
                expected.add(path.stem)
    jar = artifact()
    command, rows = invoke(jar, source, out)
    selected = [row for row in rows if Path(row["file"]).stem in expected and row["class"].split(".")[-1] in expected]
    found = {Path(row["file"]).stem for row in selected}
    if not expected or found != expected:
        raise ValueError(f"Incomplete principal-layer coverage: expected {sorted(expected)}, found {sorted(found)}")
    selected.sort(key=lambda row: (row["file"], row["class"]))
    with (out / "ck.csv").open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(selected)
    meta = {"tool": "CK", "version": VERSION, "jar_url": URL, "jar_sha256": SHA256,
            "command": command, "source_sha256": hashes,
            "git_head": subprocess.check_output(["git", "-C", str(repo), "rev-parse", "HEAD"], text=True).strip(),
            "generated_utc": datetime.now(timezone.utc).isoformat(),
            "java_version": subprocess.run(["java", "-version"], capture_output=True, text=True).stderr,
            "script_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
            "scope": "explicit public classes in service, security, controller and entity packages",
            "limits": "no dependency JAR resolution; records, enums and generated code excluded from interpreted CK scope; raw output retained"}
    (out / "metadata.json").write_text(json.dumps(meta, indent=2) + "\n", encoding="utf-8")
    print(f"CK output preserved in {out}; inspect ck.log and pass original CSV files to the coordinator")


if __name__ == "__main__":
    main()
