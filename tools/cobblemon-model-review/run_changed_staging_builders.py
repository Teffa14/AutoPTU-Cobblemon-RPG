#!/usr/bin/env python3
"""Execute changed deterministic staging builders in a pull request."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path

BUILDER_RE = re.compile(r"^tools/cobblemon-model-review/build_.+_v\d+\.py$")


def git(*args: str, cwd: Path) -> str:
    result = subprocess.run(["git", *args], cwd=cwd, check=True, text=True, capture_output=True)
    return result.stdout


def changed_builders(repo_root: Path, base: str, head: str) -> list[Path]:
    names = git("diff", "--name-only", base, head, cwd=repo_root).splitlines()
    return [repo_root / name for name in names if BUILDER_RE.match(name)]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", type=Path, default=Path("."))
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    args = parser.parse_args()

    root = args.repo_root.resolve()
    builders = changed_builders(root, args.base, args.head)
    if not builders:
        print(json.dumps({"changedStagingBuilders": [], "status": "NOT_APPLICABLE"}))
        return

    reports: list[dict] = []
    with tempfile.TemporaryDirectory(prefix="ouros-staging-builders-") as temp:
        temp_root = Path(temp)
        for index, builder in enumerate(builders):
            output_dir = temp_root / f"builder-{index}"
            command = [sys.executable, str(builder.relative_to(root)), "--repo-root", str(root), "--output-dir", str(output_dir)]
            completed = subprocess.run(command, cwd=root, text=True, capture_output=True)
            if completed.returncode != 0:
                print(json.dumps({
                    "builder": str(builder.relative_to(root)),
                    "returnCode": completed.returncode,
                    "stdout": completed.stdout,
                    "stderr": completed.stderr,
                    "status": "FAIL"
                }, indent=2), file=sys.stderr)
                raise SystemExit(completed.returncode)
            report_path = output_dir / "build-report.json"
            if not report_path.is_file():
                raise SystemExit(f"changed staging builder did not emit build-report.json: {builder}")
            report = json.loads(report_path.read_text(encoding="utf-8"))
            reports.append({"builder": str(builder.relative_to(root)), "stdout": completed.stdout.strip(), "report": report})

    print(json.dumps({"changedStagingBuilders": reports, "status": "PASS"}, indent=2))


if __name__ == "__main__":
    main()
