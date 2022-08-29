import { ColumnDescription } from "../../api/types";

export const isConceptColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.length > 0 &&
  columnDescription.semantics.some((s) => s.type === "CONCEPT_COLUMN");

export const isMoneyColumn = (columnDescription: ColumnDescription) =>
  columnDescription.type === "MONEY";

export const isSecondaryIdColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.length > 0 &&
  columnDescription.semantics.some((s) => s.type === "SECONDARY_ID");
