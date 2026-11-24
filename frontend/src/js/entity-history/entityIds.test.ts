import { describe, expect, it } from "vitest";
import type { ColumnDescription } from "../api/types";
import { collectEntityIds, pickEntityId } from "./entityIds";

const column = (
  label: string,
  semantics: ColumnDescription["semantics"],
): ColumnDescription => ({
  label,
  defaultLabel: label,
  description: null,
  selectId: null,
  userConceptLabel: null,
  type: "STRING",
  semantics,
});

const idColumn = (kind: string) => column(kind, [{ type: "ID", kind }]);
const valueColumn = column("value", []);

const columns = [idColumn("A"), idColumn("B"), valueColumn];

describe("collectEntityIds", () => {
  it("takes the first prioritized id that has a value", () => {
    expect(
      collectEntityIds(
        [
          ["a1", "b1", "x"],
          ["", "b2", "x"],
        ],
        columns,
        ["A", "B"],
      ),
    ).toEqual([
      { kind: "A", id: "a1" },
      { kind: "B", id: "b2" },
    ]);
  });

  it("follows the backend order, not the column order", () => {
    expect(collectEntityIds([["a1", "b1", "x"]], columns, ["B", "A"])).toEqual([
      { kind: "B", id: "b1" },
    ]);
  });

  it("ignores prioritized ids without a column", () => {
    expect(
      collectEntityIds([["a1", "b1", "x"]], columns, ["MISSING", "A"]),
    ).toEqual([{ kind: "A", id: "a1" }]);
  });

  it("skips rows without any id and drops duplicates", () => {
    expect(
      collectEntityIds(
        [
          ["", "", "x"],
          ["a1", "", "x"],
          ["a1", "b1", "y"],
          ["", "a1", "z"],
        ],
        columns,
        ["A", "B"],
      ),
    ).toEqual([
      { kind: "A", id: "a1" },
      { kind: "B", id: "a1" },
    ]);
  });
});

describe("pickEntityId", () => {
  it("prefers the earlier kind and skips empty ones", () => {
    expect(pickEntityId({ A: "", B: "b1" }, ["A", "B"])).toEqual({
      kind: "B",
      id: "b1",
    });
    expect(pickEntityId({ B: "b1", A: "a1" }, ["A", "B"])).toEqual({
      kind: "A",
      id: "a1",
    });
  });

  it("returns null when no prioritized kind has a value", () => {
    expect(pickEntityId({ C: "c1" }, ["A", "B"])).toBeNull();
  });
});
