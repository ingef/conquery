import { ColumnDescription, TimeStratifiedInfo } from "../../api/types";

export const isIdColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.some((s) => s.type === "ID");

export const isDateColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.some((s) => s.type === "EVENT_DATE");

export const isGroupableColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.some((s) => s.type === "GROUP");

export const isVisibleColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.length === 0 ||
  columnDescription.semantics.every((s) => s.type !== "HIDDEN");

export const isConceptColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.some((s) => s.type === "CONCEPT_COLUMN");

export const isMoneyColumn = (columnDescription: ColumnDescription) =>
  columnDescription.type === "MONEY";

export const isSecondaryIdColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.some((s) => s.type === "SECONDARY_ID");

export const getColumnType = (
  timeStratifiedInfo: TimeStratifiedInfo,
  label: string,
) => {
  return timeStratifiedInfo.columns.find((c) => c.label === label)?.type;
};
