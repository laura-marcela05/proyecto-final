"""Maintenance effort from real records, labeled issues and optional own commits."""
import argparse
import csv
from datetime import datetime, timezone, date
from decimal import Decimal
import hashlib
import json
from pathlib import Path
import re
import subprocess
import urllib.request

REPOSITORY = "laura-marcela05/proyecto-final"
TYPES = ("correctivo", "adaptativo", "perfectivo", "preventivo")
PEOPLE = {"Juan": {"jcbernal-2024a@corhuila.edu.co"},
          "Laura": {"lmquintero-2024a@corhuila.edu.co"},
          "Cristian": {"lusami2005@gmail.com"}}
FIELDS = ["record_id", "issue_number", "type", "person", "date", "hours", "basis",
          "hourly_rate", "commit", "description"]


def read_records(path):
    with Path(path).open(encoding="utf-8-sig", newline="") as stream:
        reader = csv.DictReader(stream)
        if reader.fieldnames != FIELDS:
            raise ValueError("Unexpected record columns; use the supplied header")
        return list(reader)


def number(value, name):
    result = Decimal(value)
    if not result.is_finite() or result < 0:
        raise ValueError(f"Invalid {name}")
    return result


def summarize(records, issues, commit_author):
    if not records:
        raise ValueError("No maintenance records: pending, not zero cost. Do not invent hours")
    by_issue = {int(issue["number"]): issue for issue in issues if "pull_request" not in issue}
    seen, output = set(), []
    for original in records:
        row = {key: value.strip() for key, value in original.items()}
        if not row["record_id"] or row["record_id"] in seen:
            raise ValueError("Missing or duplicate record_id")
        seen.add(row["record_id"])
        if row["type"] not in TYPES or row["person"] not in PEOPLE:
            raise ValueError("Unknown maintenance type or person")
        date.fromisoformat(row["date"])
        if row["basis"] not in {"recorded", "retrospective_estimate"} or not row["description"]:
            raise ValueError("Declare time evidence/basis and describe the actual work")
        issue = by_issue.get(int(row["issue_number"]))
        if issue is None:
            raise ValueError(f"Issue {row['issue_number']} missing from snapshot")
        labels = {label["name"] if isinstance(label, dict) else label for label in issue["labels"]}
        if labels.intersection(TYPES) != {row["type"]}:
            raise ValueError(f"Issue {row['issue_number']} needs exactly one matching maintenance label")
        if row["commit"]:
            if not re.fullmatch(r"[a-fA-F0-9]{7,40}", row["commit"]):
                raise ValueError("Commit must be a real Git SHA, not a PR number")
            if commit_author(row["commit"]).lower() not in PEOPLE[row["person"]]:
                raise ValueError("Commit author does not match record owner; do not attribute another person's commit")
        hours = number(row["hours"], "hours")
        if hours == 0:
            raise ValueError("No-work entries are not maintenance records")
        rate = number(row["hourly_rate"], "hourly_rate") if row["hourly_rate"] else None
        output.append({**row, "hours": hours, "cost": hours * rate if rate is not None else None})
    total = sum((row["hours"] for row in output), Decimal(0))
    summary = []
    for kind in TYPES:
        selected = [row for row in output if row["type"] == kind]
        hours = sum((row["hours"] for row in selected), Decimal(0))
        summary.append({"type": kind, "hours": str(hours), "effort_percent": str(hours * 100 / total),
                        "cost": str(sum((row["cost"] for row in selected), Decimal(0)))
                        if selected and all(row["cost"] is not None for row in selected) else None,
                        "records": len(selected)})
    logged = {int(row["issue_number"]) for row in output}
    unlogged = sorted(number for number, issue in by_issue.items()
                      if any((label["name"] if isinstance(label, dict) else label) in TYPES for label in issue["labels"])
                      and number not in logged)
    return {"hours_total": str(total), "types": summary,
            "cost_total": str(sum((row["cost"] for row in output), Decimal(0)))
            if all(row["cost"] is not None for row in output) else None,
            "has_estimates": any(row["basis"] == "retrospective_estimate" for row in output),
            "unlogged_labeled_issues": unlogged,
            "coverage_warning": "Distribution describes recorded effort only; unlogged work is unknown, not zero",
            "currency": "not assigned by the script; document the common currency externally if rates are supplied"}


def export_issues(path):
    path = Path(path)
    if path.exists():
        raise ValueError("Snapshot already exists; preserve evidence")
    issues, page = [], 1
    while True:
        request = urllib.request.Request(
            f"https://api.github.com/repos/{REPOSITORY}/issues?state=all&per_page=100&page={page}",
            headers={"Accept": "application/vnd.github+json", "User-Agent": "gym-track-maintenance/1.0",
                     "X-GitHub-Api-Version": "2026-03-10"})
        with urllib.request.urlopen(request, timeout=45) as response:
            batch = json.load(response)
        if not isinstance(batch, list):
            raise ValueError("Invalid GitHub issue response")
        issues.extend(issue for issue in batch if "pull_request" not in issue)
        if len(batch) < 100:
            break
        page += 1
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps({"repository": REPOSITORY,
                    "snapshot_utc": datetime.now(timezone.utc).isoformat(), "issues": issues}, indent=2) + "\n", encoding="utf-8")
    print(f"Saved public issue snapshot to {path}; no issue was created or modified")


def self_test():
    fixture = {key: "" for key in FIELDS}
    fixture.update(record_id="fixture", issue_number="1", type="preventivo", person="Juan",
                   date="2026-01-01", hours="2", basis="recorded", description="Synthetic fixture only")
    issue = {"number": 1, "labels": [{"name": "preventivo"}]}
    result = summarize([fixture], [issue], lambda sha: "")
    assert result["hours_total"] == "2" and result["cost_total"] is None
    assert result["types"][-1]["effort_percent"] == "100"
    try:
        summarize([fixture, fixture], [issue], lambda sha: "")
    except ValueError:
        pass
    else:
        raise AssertionError("Duplicate records must fail")
    try:
        summarize([], [], lambda sha: "")
    except ValueError:
        pass
    else:
        raise AssertionError("Missing records must not become zero cost")
    print("SELF-TEST OK: effort, missing rates, duplicates and missing evidence")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--self-test", action="store_true")
    ap.add_argument("--export-issues")
    ap.add_argument("--repo", default=".")
    ap.add_argument("--records", default="metricas/mantenimiento/registro.csv")
    ap.add_argument("--issues")
    ap.add_argument("--out")
    args = ap.parse_args()
    if args.self_test:
        self_test()
        return
    if args.export_issues:
        export_issues(args.export_issues)
        return
    if not args.issues or not args.out:
        ap.error("--issues and --out are required")
    repo, out = Path(args.repo).resolve(), Path(args.out).resolve()
    if out.exists():
        raise ValueError("Output exists: preserve previous evidence")
    snapshot = json.loads(Path(args.issues).read_text(encoding="utf-8"))
    if snapshot.get("repository") != REPOSITORY or not snapshot.get("snapshot_utc"):
        raise ValueError("Wrong repository or missing snapshot date")
    def commit_author(sha):
        subprocess.run(["git", "-C", str(repo), "merge-base", "--is-ancestor", sha, "HEAD"], check=True)
        return subprocess.check_output(["git", "-C", str(repo), "show", "-s", "--format=%ae", sha], text=True).strip()
    result = summarize(read_records(args.records), snapshot["issues"], commit_author)
    result["metadata"] = {"generated_utc": datetime.now(timezone.utc).isoformat(),
                          "git_head": subprocess.check_output(["git", "-C", str(repo), "rev-parse", "HEAD"], text=True).strip(),
                          "records_sha256": hashlib.sha256(Path(args.records).read_bytes()).hexdigest(),
                          "issues_sha256": hashlib.sha256(Path(args.issues).read_bytes()).hexdigest(),
                          "script_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                          "tool": "gym-track-maintenance/1.0"}
    out.mkdir(parents=True)
    (out / "maintenance.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(f"Written maintenance.json to {out}; interpretation pending real team evidence")


if __name__ == "__main__":
    main()
