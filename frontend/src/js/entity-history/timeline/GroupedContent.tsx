import { css, Theme } from "@emotion/react";
import styled from "@emotion/styled";
import { memo, ReactNode, useMemo } from "react";
import { NumericFormat } from "react-number-format";

import {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
} from "../../api/types";
import { ContentFilterValue, ContentType } from "../ContentControl";
import { formatHistoryDayRange } from "../RowDates";
import { EntityEvent } from "../reducer";

import ConceptName from "./ConceptName";
import { TinyLabel } from "./TinyLabel";
import {
  isConceptColumn,
  isDateColumn,
  isMoneyColumn,
  isSecondaryIdColumn,
  isVisibleColumn,
} from "./util";

const Grid = styled("div")`
  display: inline-grid;
  gap: 5px 10px;
`;

const ExtraArea = styled("div")`
  padding: 8px 15px 12px 49px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
`;

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
    <ExtraArea>
      <Grid
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
      </Grid>
    </ExtraArea>
  );
};

const cellStyles = (theme: Theme) => css`
  white-space: nowrap;
  font-size: ${theme.font.sm};
`;
const CellWrap = styled("span")`
  ${({ theme }) => cellStyles(theme)};
`;
const SxConceptName = styled(ConceptName)`
  ${({ theme }) => cellStyles(theme)};
`;
const SxNumericFormat = styled(NumericFormat)`
  ${({ theme }) => cellStyles(theme)};
`;

interface DateRow {
  from: string;
  to: string;
}
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
        <CellWrap>{formatHistoryDayRange((cell as DateRow).from)}</CellWrap>
      ) : (
        <CellWrap>
          {formatHistoryDayRange((cell as DateRow).from)} -{" "}
          {formatHistoryDayRange((cell as DateRow).to)}
        </CellWrap>
      );
    }

    if (isConceptColumn(columnDescription)) {
      return (
        <SxConceptName
          rootConceptId={rootConceptIdsByColumn[columnDescription.label]}
          conceptId={cell as string}
          title={columnDescription.defaultLabel}
        />
      );
    }

    if (isMoneyColumn(columnDescription)) {
      return (
        <SxNumericFormat
          {...currencyConfig}
          displayType="text"
          value={parseInt(cell as string) / 100}
        />
      );
    }

    return <CellWrap>{cell as ReactNode}</CellWrap>;
  },
);

export default GroupedContent;
