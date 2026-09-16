#!/usr/bin/env python3
"""Normalize connector, filter, select, and validity-date columns in concept JSON files.

Connector columns of the form "table.column" become:
  "table": "table",
  "column": "column"

Filter columns of the form "table.column" become:
  "column": "column"

Connector-select column references become local column names as well. This includes
column, startColumn, endColumn, subtractColumn, distinctByColumn, distinctBy, and
the values in flags maps.

Validity-date column, startColumn, and endColumn references become local column
names as well.
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


def normalize_column(value: Any) -> tuple[Any, int]:
    split = split_table_column(value)
    if split is None:
        return value, 0
    return split[1], 1


def normalize_column_collection(value: Any) -> tuple[Any, int]:
    if isinstance(value, list):
        changed = 0
        normalized = []
        for item in value:
            normalized_item, item_changes = normalize_column(item)
            normalized.append(normalized_item)
            changed += item_changes
        return normalized, changed
    return normalize_column(value)


def normalize_definition_columns(
    value: Any,
    scalar_fields: tuple[str, ...],
    collection_fields: tuple[str, ...],
) -> int:
    changed = 0
    if isinstance(value, dict):
        for field in scalar_fields:
            if field in value:
                value[field], field_changes = normalize_column(value[field])
                changed += field_changes

        for field in collection_fields:
            if field in value:
                value[field], field_changes = normalize_column_collection(value[field])
                changed += field_changes

        flags = value.get("flags")
        if isinstance(flags, dict):
            for key, column in flags.items():
                flags[key], field_changes = normalize_column(column)
                changed += field_changes

        for child in value.values():
            changed += normalize_definition_columns(child, scalar_fields, collection_fields)
    elif isinstance(value, list):
        for child in value:
            changed += normalize_definition_columns(child, scalar_fields, collection_fields)
    return changed


def normalize_filter_columns(value: Any) -> int:
    return normalize_definition_columns(value, ("column",), ("distinctByColumn",))


def normalize_select_columns(value: Any) -> int:
    return normalize_definition_columns(
        value,
        ("column", "startColumn", "endColumn", "subtractColumn"),
        ("distinctByColumn", "distinctBy"),
    )


def normalize_validity_date_columns(value: Any) -> int:
    return normalize_definition_columns(value, ("column", "startColumn", "endColumn"), ())


def normalize_connectors(value: Any) -> tuple[int, int, int, int]:
    connector_changes = 0
    filter_changes = 0
    select_changes = 0
    validity_date_changes = 0

    if isinstance(value, dict):
        connectors = value.get("connectors")
        connector_definitions = connectors if isinstance(connectors, list) else [connectors]
        for connector in connector_definitions:
            if not isinstance(connector, dict):
                continue

            split = split_table_column(connector.get("column"))
            if split is not None:
                set_connector_table_column(connector, split[0], split[1])
                connector_changes += 1

            filter_changes += normalize_filter_columns(connector.get("filters"))
            select_changes += normalize_select_columns(connector.get("selects"))
            validity_date_changes += normalize_validity_date_columns(connector.get("validityDates"))

        for child in value.values():
            child_connector_changes, child_filter_changes, child_select_changes, child_validity_date_changes = normalize_connectors(child)
            connector_changes += child_connector_changes
            filter_changes += child_filter_changes
            select_changes += child_select_changes
            validity_date_changes += child_validity_date_changes

    elif isinstance(value, list):
        for child in value:
            child_connector_changes, child_filter_changes, child_select_changes, child_validity_date_changes = normalize_connectors(child)
            connector_changes += child_connector_changes
            filter_changes += child_filter_changes
            select_changes += child_select_changes
            validity_date_changes += child_validity_date_changes

    return connector_changes, filter_changes, select_changes, validity_date_changes


def normalize_file(path: Path, dry_run: bool) -> tuple[int, int, int, int]:
    data = json.loads(path.read_text(encoding="utf-8"))
    connector_changes, filter_changes, select_changes, validity_date_changes = normalize_connectors(data)

    if not dry_run and (connector_changes or filter_changes or select_changes or validity_date_changes):
        path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    return connector_changes, filter_changes, select_changes, validity_date_changes


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize table.column references in concept JSON files.")
    parser.add_argument("concept_json", nargs="+", type=Path, help="Concept JSON file(s) to rewrite in place.")
    parser.add_argument("--dry-run", action="store_true", help="Report changes without writing files.")
    args = parser.parse_args()

    for path in args.concept_json:
        connector_changes, filter_changes, select_changes, validity_date_changes = normalize_file(path, args.dry_run)
        action = "would update" if args.dry_run else "updated"
        print(
            f"{path}: {action} {connector_changes} connector column(s), "
            f"{filter_changes} filter column(s), {select_changes} select column(s), "
            f"{validity_date_changes} validity-date column(s)"
        )

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
