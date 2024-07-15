import { useMemo } from "react";
import { useSelector } from "react-redux";
import {
  ColumnDescription,
  ColumnDescriptionSemanticConceptColumn,
  ConceptIdT,
} from "../../../api/types";
import type { StateT } from "../../../app/reducers";
import { EntityHistoryStateT } from "../../reducer";
import {
  isConceptColumn,
  isDateColumn,
  isGroupableColumn,
  isIdColumn,
  isMoneyColumn,
  isSecondaryIdColumn,
  isSourceColumn,
  isVisibleColumn,
} from "./util";

export interface ColumnBuckets {
  money: ColumnDescription[];
  concepts: ColumnDescription[];
  secondaryIds: ColumnDescription[];
  rest: ColumnDescription[];
  groupableIds: ColumnDescription[];
}
export const useColumnInformation = () => {
  const columnDescriptions = useSelector<StateT, ColumnDescription[]>(
    (state) => state.entityHistory.columnDescriptions,
  );

  const columns = useSelector<StateT, EntityHistoryStateT["columns"]>(
    (state) => state.entityHistory.columns,
  );

  const dateColumn = useMemo(
    () => Object.values(columns).find(isDateColumn),
    [columns],
  );

  const sourceColumn = useMemo(
    () => Object.values(columns).find(isSourceColumn),
    [columns],
  );

  const columnBuckets: ColumnBuckets = useMemo(() => {
    const visibleColumnDescriptions =
      columnDescriptions.filter(isVisibleColumn);

    return {
      money: visibleColumnDescriptions.filter(isMoneyColumn),
      concepts: visibleColumnDescriptions.filter(isConceptColumn),
      secondaryIds: visibleColumnDescriptions.filter(isSecondaryIdColumn),
      groupableIds: visibleColumnDescriptions.filter(isGroupableColumn),
      rest: visibleColumnDescriptions.filter(
        (c) =>
          !isMoneyColumn(c) &&
          (c.semantics.length === 0 ||
            (!isGroupableColumn(c) && !isIdColumn(c))),
      ),
    };
  }, [columnDescriptions]);

  const rootConceptIdsByColumn: Record<string, ConceptIdT> = useMemo(() => {
    const entries: [string, ConceptIdT][] = [];

    for (const columnDescription of columnBuckets.concepts) {
      const { label, semantics } = columnDescription;
      const conceptSemantic = semantics.find(
        (sem): sem is ColumnDescriptionSemanticConceptColumn =>
          sem.type === "CONCEPT_COLUMN",
      );

      if (conceptSemantic) {
        entries.push([label, conceptSemantic.concept]);
      }
    }

    return Object.fromEntries(entries);
  }, [columnBuckets]);

  return {
    columns,
    columnBuckets,
    dateColumn,
    sourceColumn,
    rootConceptIdsByColumn,
  };
};
