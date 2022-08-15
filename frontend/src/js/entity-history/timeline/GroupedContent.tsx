import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import NumberFormat from "react-number-format";

import {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
} from "../../api/types";
import { EntityEvent } from "../reducer";

import ConceptName from "./ConceptName";
import { TinyLabel } from "./TinyLabel";

const Grid = styled("div")`
  display: inline-grid;
  gap: 5px 10px;
`;

const ExtraArea = styled("div")`
  padding: 12px 12px 12px 40px;
  background-color: ${({ theme }) => theme.col.bg};
  border-top: 1px solid ${({ theme }) => theme.col.grayVeryLight};
`;

const ExtraAreaHeading = styled("h5")`
  font-size: ${({ theme }) => theme.font.xs};
  font-weight: 700;
  margin: 0 0 8px;
`;

const isConceptColumn = (columnDescription: ColumnDescription) =>
  columnDescription.semantics.length > 0 &&
  columnDescription.semantics[0].type === "CONCEPT_COLUMN";

const isMoneyColumn = (columnDescription: ColumnDescription) =>
  columnDescription.type === "MONEY";

interface Props {
  datasetId: DatasetT["id"];
  columns: Record<string, ColumnDescription>;
  groupedRows: EntityEvent[];
  groupedRowsDifferences: Record<string, Set<any>>;
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
}
const GroupedContent = ({
  datasetId,
  columns,
  groupedRows,
  groupedRowsDifferences,
  currencyConfig,
  rootConceptIdsByColumn,
}: Props) => {
  const { t } = useTranslation();
  const differencesKeys = Object.keys(groupedRowsDifferences);

  return (
    <ExtraArea>
      <ExtraAreaHeading>
        {groupedRows.length}{" "}
        {t("history.events", { count: groupedRows.length })}
      </ExtraAreaHeading>
      <Grid
        style={{
          gridTemplateColumns: `repeat(${differencesKeys.length}, auto)`,
        }}
      >
        {differencesKeys.map((key) => (
          <TinyLabel>{key}</TinyLabel>
        ))}
        {groupedRows.map((groupedRow) =>
          differencesKeys.map((key) => {
            const columnDescription = columns[key];

            if (isConceptColumn(columnDescription)) {
              return (
                <ConceptName
                  rootConceptId={
                    rootConceptIdsByColumn[columnDescription.label]
                  }
                  conceptId={groupedRow[key]}
                  datasetId={datasetId}
                  title={columnDescription.label}
                />
              );
            }

            if (isMoneyColumn(columnDescription)) {
              return (
                <NumberFormat
                  {...currencyConfig}
                  displayType="text"
                  value={parseInt(groupedRow[key]) / 100}
                />
              );
            }

            return <div>{groupedRow[key]}</div>;
          }),
        )}
      </Grid>
    </ExtraArea>
  );
};

export default GroupedContent;
