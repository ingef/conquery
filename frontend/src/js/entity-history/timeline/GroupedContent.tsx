import { memo, type ReactNode, useMemo } from "react";
import { tv } from "tailwind-variants";

import type { ColumnDescription, ConceptIdT } from "../../api/types";
import { useMoneyFormatter } from "../../common/useMoneyFormatter";
import { C2 } from "../../ui-components/Typography";
import type { ContentFilterValue, ContentType } from "../ContentControl";
import { formatHistoryDayRange } from "../RowDates";
import type { DateRow, EntityEvent } from "../reducer";
import ConceptName from "./ConceptName";
import { TinyLabel } from "./TinyLabel";
import {
  isConceptColumn,
  isDateColumn,
  isMoneyColumn,
  isSecondaryIdColumn,
  isVisibleColumn,
} from "./util/util";

const grid = tv({
  base: ["inline-grid", "gap-x-[10px] gap-y-[5px]", "whitespace-nowrap"],
});

// indented past the bucket icons above and below
const extraArea = tv({
  base: ["pl-[43px]", "overflow-x-auto", "[-webkit-overflow-scrolling:touch]"],
});

const getColumnDescriptionContentType = (
  columnDescription?: ColumnDescription,
): ContentType => {
  if (!columnDescription) {
    return "dates";
  }

  if (isMoneyColumn(columnDescription)) {
    return "money";
  } else if (isConceptColumn(columnDescription)) {
    return "concept";
  } else if (isSecondaryIdColumn(columnDescription)) {
    return "secondaryId";
  } else {
    return "rest";
  }
};

const SORT_ORDER: ContentType[] = ["concept", "secondaryId", "rest", "money"];
interface Props {
  columns: Record<string, ColumnDescription>;
  groupedRows: EntityEvent[];
  groupedRowsKeysWithDifferentValues: string[];
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  contentFilter: ContentFilterValue;
}

const GroupedContent = ({
  columns,
  groupedRows,
  groupedRowsKeysWithDifferentValues,
  rootConceptIdsByColumn,
  contentFilter,
}: Props) => {
  const differencesKeys = useMemo(
    () =>
      groupedRowsKeysWithDifferentValues
        .filter((key) => {
          if (isDateColumn(columns[key])) return true;

          if (!isVisibleColumn(columns[key])) {
            return false;
          }
          const columnType = getColumnDescriptionContentType(columns[key]);

          return contentFilter[columnType];
        })
        .sort(
          (a, b) =>
            SORT_ORDER.indexOf(getColumnDescriptionContentType(columns[a])) -
            SORT_ORDER.indexOf(getColumnDescriptionContentType(columns[b])),
        ),
    [columns, groupedRowsKeysWithDifferentValues, contentFilter],
  );

  if (differencesKeys.length === 0) {
    return null;
  }

  return (
    <div className={extraArea()}>
      <div
        className={grid()}
        style={{
          gridTemplateColumns: `repeat(${differencesKeys.length}, auto)`,
        }}
      >
        {differencesKeys.map((key) => (
          <TinyLabel key={key}>{columns[key].defaultLabel}</TinyLabel>
        ))}
        {groupedRows.map((groupedRow) =>
          differencesKeys.map((key) => (
            <Cell
              key={key}
              columnDescription={columns[key]}
              cell={groupedRow[key]}
              rootConceptIdsByColumn={rootConceptIdsByColumn}
            />
          )),
        )}
      </div>
    </div>
  );
};

const Cell = memo(
  ({
    columnDescription,
    cell,
    rootConceptIdsByColumn,
  }: {
    columnDescription: ColumnDescription;
    cell: unknown;
    rootConceptIdsByColumn: Record<string, ConceptIdT>;
  }) => {
    const moneyFormatter = useMoneyFormatter();

    if (isDateColumn(columnDescription)) {
      return (cell as DateRow).from === (cell as DateRow).to ? (
        <C2 as="span">{formatHistoryDayRange((cell as DateRow).from)}</C2>
      ) : (
        <C2 as="span">
          {formatHistoryDayRange((cell as DateRow).from)} -{" "}
          {formatHistoryDayRange((cell as DateRow).to)}
        </C2>
      );
    }

    if (isConceptColumn(columnDescription)) {
      return (
        <ConceptName
          rootConceptId={rootConceptIdsByColumn[columnDescription.label]}
          conceptId={cell as string}
          title={columnDescription.defaultLabel}
        />
      );
    }

    if (isMoneyColumn(columnDescription)) {
      return (
        <C2 as="span">{moneyFormatter.format(parseFloat(cell as string))}</C2>
      );
    }

    return <C2 as="span">{cell as ReactNode}</C2>;
  },
);

export default GroupedContent;
