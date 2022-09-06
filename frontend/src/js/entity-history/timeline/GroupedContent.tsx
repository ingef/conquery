import { css, Theme } from "@emotion/react";
import styled from "@emotion/styled";
import { memo, useMemo } from "react";
import NumberFormat from "react-number-format";

import {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
} from "../../api/types";
import { ContentFilterValue, ContentType } from "../ContentControl";
import { EntityEvent } from "../reducer";

import ConceptName from "./ConceptName";
import { TinyLabel } from "./TinyLabel";
import {
  isConceptColumn,
  isMoneyColumn,
  isSecondaryIdColumn,
  isVisibleColumn,
} from "./util";

const Grid = styled("div")`
  display: inline-grid;
  gap: 5px 10px;
`;

const ExtraArea = styled("div")`
  padding: 12px 12px 12px 40px;
  background-color: ${({ theme }) => theme.col.bg};
  border-top: 1px solid ${({ theme }) => theme.col.grayVeryLight};
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
`;

const getColumnDescriptionContentType = (
  columnDescription: ColumnDescription,
): ContentType => {
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
  datasetId: DatasetT["id"];
  columns: Record<string, ColumnDescription>;
  groupedRows: EntityEvent[];
  groupedRowsKeysWithDifferentValues: string[];
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  contentFilter: ContentFilterValue;
}

const GroupedContent = ({
  datasetId,
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
          <TinyLabel>{columns[key].defaultLabel}</TinyLabel>
        ))}
        {groupedRows.map((groupedRow) =>
          differencesKeys.map((key) => (
            <Cell
              key={key}
              datasetId={datasetId}
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
const SxNumberFormat = styled(NumberFormat)`
  ${({ theme }) => cellStyles(theme)};
`;
const Cell = memo(
  ({
    columnDescription,
    currencyConfig,
    cell,
    datasetId,
    rootConceptIdsByColumn,
  }: {
    columnDescription: ColumnDescription;
    currencyConfig: CurrencyConfigT;
    cell: any;
    datasetId: DatasetT["id"];
    rootConceptIdsByColumn: Record<string, ConceptIdT>;
  }) => {
    if (isConceptColumn(columnDescription)) {
      return (
        <SxConceptName
          rootConceptId={rootConceptIdsByColumn[columnDescription.label]}
          conceptId={cell}
          datasetId={datasetId}
          title={columnDescription.defaultLabel}
        />
      );
    }

    if (isMoneyColumn(columnDescription)) {
      return (
        <SxNumberFormat
          {...currencyConfig}
          displayType="text"
          value={parseInt(cell) / 100}
        />
      );
    }

    return <CellWrap>{cell}</CellWrap>;
  },
);

export default GroupedContent;
