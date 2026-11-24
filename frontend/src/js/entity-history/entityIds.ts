import type { ColumnDescription } from "../api/types";

import type { EntityId } from "./reducer";

const findIdColumns = (
  columns: ColumnDescription[],
  prioritizedIdColumns: string[],
) =>
  prioritizedIdColumns.flatMap((kind) => {
    const columnIdx = columns.findIndex((col) =>
      col.semantics.some((s) => s.type === "ID" && s.kind === kind),
    );

    return columnIdx === -1 ? [] : [{ kind, columnIdx }];
  });

// One entity per result row, identified by its first non-empty id in the backend's priority order
export function collectEntityIds(
  rows: string[][],
  columns: ColumnDescription[],
  prioritizedIdColumns: string[],
): EntityId[] {
  const idColumns = findIdColumns(columns, prioritizedIdColumns);
  const seen = new Set<string>();
  const entityIds: EntityId[] = [];

  for (const row of rows) {
    const idColumn = idColumns.find((col) => row[col.columnIdx]);

    if (!idColumn) continue;

    const entityId = { kind: idColumn.kind, id: row[idColumn.columnIdx] };
    const key = `${entityId.kind}:${entityId.id}`;

    if (seen.has(key)) continue;

    seen.add(key);
    entityIds.push(entityId);
  }

  return entityIds;
}

export function pickEntityId(
  idsByKind: Record<string, string>,
  prioritizedIdColumns: string[],
): EntityId | null {
  const kind = prioritizedIdColumns.find((k) => idsByKind[k]);

  return kind ? { kind, id: idsByKind[kind] } : null;
}
