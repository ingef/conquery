import { memo, type ReactNode, useMemo } from "react";
import {
  type InputAttributes,
  NumericFormat,
  type NumericFormatProps,
} from "react-number-format";
import { tv } from "tailwind-variants";

import type {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
} from "../../api/types";
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
  base: ["inline-grid", "gap-x-[10px] gap-y-[5px]"],
});

const extraArea = tv({
  base: [
    "pt-2 pr-[15px] pb-3 pl-[49px]",
    "overflow-x-auto",
    "[-webkit-overflow-scrolling:touch]",
  ],
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
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  contentFilter: ContentFilterValue;
}

const GroupedContent = ({
  columns,
  groupedRows,
  groupedRowsKeysWithDifferentValues,
  currencyConfig,
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
              currencyConfig={currencyConfig}
              rootConceptIdsByColumn={rootConceptIdsByColumn}
            />
          )),
        )}
      </div>
    </div>
  );
};

// named cellText, not cell: the Cell component's `cell` prop would shadow it
const cellText = tv({
  base: ["whitespace-nowrap", "text-sm"],
});

const Cell = memo(
  ({
    columnDescription,
    currencyConfig,
    cell,
    rootConceptIdsByColumn,
  }: {
    columnDescription: ColumnDescription;
    currencyConfig: CurrencyConfigT;
    cell: unknown;
    rootConceptIdsByColumn: Record<string, ConceptIdT>;
  }) => {
    if (isDateColumn(columnDescription)) {
      return (cell as DateRow).from === (cell as DateRow).to ? (
        <span className={cellText()}>
          {formatHistoryDayRange((cell as DateRow).from)}
        </span>
      ) : (
        <span className={cellText()}>
          {formatHistoryDayRange((cell as DateRow).from)} -{" "}
          {formatHistoryDayRange((cell as DateRow).to)}
        </span>
      );
    }

    if (isConceptColumn(columnDescription)) {
      return (
        <ConceptName
          className={cellText()}
          rootConceptId={rootConceptIdsByColumn[columnDescription.label]}
          conceptId={cell as string}
          title={columnDescription.defaultLabel}
        />
      );
    }

    if (isMoneyColumn(columnDescription)) {
      // typed up front with an explicit base type: the polymorphic NumericFormat
      // props are otherwise too complex a union for tsc
      const numericFormatProps: NumericFormatProps<InputAttributes> = {
        ...currencyConfig,
        className: cellText(),
        displayType: "text",
        value: parseFloat(cell as string),
      };
      return <NumericFormat<InputAttributes> {...numericFormatProps} />;
    }

    return <span className={cellText()}>{cell as ReactNode}</span>;
  },
);

export default GroupedContent;
