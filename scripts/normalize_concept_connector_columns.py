#!/usr/bin/env python3
"""Normalize connector and filter column references in concept JSON files.

Connector columns of the form "table.column" become:
  "table": "table",
  "column": "column"

Filter columns of the form "table.column" become:
  "column": "column"
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def split_table_column(value: Any) -> tuple[str, str] | None:
    if not isinstance(value, str):
        return None
    table, separator, column = value.partition(".")
    if not separator or not table or not column:
        return None
    return table, column


def set_connector_table_column(connector: dict[str, Any], table: str, column: str) -> None:
    reordered: dict[str, Any] = {}
    inserted = False
    for key, value in connector.items():
        if key == "column":
            reordered["table"] = table
            reordered["column"] = column
            inserted = True
        elif key != "table":
            reordered[key] = value

    if not inserted:
        reordered["table"] = table
        reordered["column"] = column

    connector.clear()
    connector.update(reordered)


def normalize_filter_columns(value: Any) -> int:
    changed = 0
    if isinstance(value, dict):
        split = split_table_column(value.get("column"))
        if split is not None:
            value["column"] = split[1]
            changed += 1

        # FLAGS filter
        flags = value.get("flags") or {}
        for key,val in flags.items():
            split = split_table_column(val)
            if split is not None:
                flags[key] = split[1]
                changed += 1

        # distinctByColumn (actually a list, but sometimes a scalar)
        distinctByColumn = value.get("distinctByColumn") or []
        if distinctByColumn:
            distinctByColumn = [distinctByColumn] if isinstance(distinctByColumn, str) else distinctByColumn
            for i, item in enumerate(distinctByColumn):
                split = split_table_column(item)
                if split is not None:
                    distinctByColumn[i] = split[1]
                    changed += 1    
            value["distinctByColumn"] = distinctByColumn

        for child in value.values():
            changed += normalize_filter_columns(child)
    elif isinstance(value, list):
        for child in value:
            changed += normalize_filter_columns(child)
    return changed


def normalize_connectors(value: Any) -> tuple[int, int]:
    connector_changes = 0
    filter_changes = 0

    if isinstance(value, dict):
        connectors = value.get("connectors")
        if isinstance(connectors, list):
            for connector in connectors:
                if not isinstance(connector, dict):
                    continue

                split = split_table_column(connector.get("column"))
                if split is not None:
                    set_connector_table_column(connector, split[0], split[1])
                    connector_changes += 1

                filter_changes += normalize_filter_columns(connector.get("filters"))

        for child in value.values():
            child_connector_changes, child_filter_changes = normalize_connectors(child)
            connector_changes += child_connector_changes
            filter_changes += child_filter_changes

    elif isinstance(value, list):
        for child in value:
            child_connector_changes, child_filter_changes = normalize_connectors(child)
            connector_changes += child_connector_changes
            filter_changes += child_filter_changes

    return connector_changes, filter_changes


def normalize_file(path: Path, dry_run: bool) -> tuple[int, int]:
    data = json.loads(path.read_text(encoding="utf-8"))
    connector_changes, filter_changes = normalize_connectors(data)

    if not dry_run and (connector_changes or filter_changes):
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    return connector_changes, filter_changes


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize table.column references in concept JSON files.")
    parser.add_argument("concept_json", nargs="+", type=Path, help="Concept JSON file(s) to rewrite in place.")
    parser.add_argument("--dry-run", action="store_true", help="Report changes without writing files.")
    args = parser.parse_args()

    for path in args.concept_json:
        connector_changes, filter_changes = normalize_file(path, args.dry_run)
        action = "would update" if args.dry_run else "updated"
        print(f"{path}: {action} {connector_changes} connector column(s), {filter_changes} filter column(s)")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
