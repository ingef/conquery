import styled from "@emotion/styled";
import NumberFormat from "react-number-format";

import type {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
} from "../../api/types";
import { exists } from "../../common/helpers/exists";
import FaIcon from "../../icon/FaIcon";
import WithTooltip from "../../tooltip/WithTooltip";
import type { ContentFilterValue } from "../ContentControl";
import { RowDates } from "../RowDates";
import type { EntityEvent } from "../reducer";

import ConceptName from "./ConceptName";
import GroupedContent from "./GroupedContent";
import { RawDataBadge } from "./RawDataBadge";
import { TinyLabel } from "./TinyLabel";

const Card = styled("div")`
  display: grid;
  grid-template-columns: auto 45px 1fr;
  gap: 3px;
  font-size: ${({ theme }) => theme.font.xs};
  padding: 5px 0;
  position: relative;
`;

const EventItemContent = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 1px 1px ${({ theme }) => theme.col.grayLight};
  margin-top: 5px;
  background-color: white;
  overflow: hidden;
`;
const MainContent = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 5px;
  padding: 15px 8px 8px;
`;

const ColBucket = styled("div")`
  color: black;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0 10px;
  padding: 1px 4px;
`;

const SxRawDataBadge = styled(RawDataBadge)`
  position: absolute;
  z-index: 10;
  top: 4px;
  left: 55px;
`;

const SxFaIcon = styled(FaIcon)`
  width: 20px !important;
  text-align: center;
`;

const ColBucketCode = styled((props: any) => (
  <ColBucket as="code" {...props} />
))``;

const Bullet = styled("div")`
  width: 10px;
  height: 10px;
  margin: 2px 0;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  flex-shrink: 0;
`;

interface ColumnBuckets {
  money: ColumnDescription[];
  concepts: ColumnDescription[];
  secondaryIds: ColumnDescription[];
  rest: ColumnDescription[];
}

interface Props {
  row: EntityEvent;
  columns: Record<string, ColumnDescription>;
  columnBuckets: ColumnBuckets;
  datasetId: DatasetT["id"];
  contentFilter: ContentFilterValue;
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  groupedRows?: EntityEvent[];
  groupedRowsDifferences?: Record<string, Set<any>>;
}

const EventCard = ({
  row,
  columns,
  columnBuckets,
  datasetId,
  currencyConfig,
  contentFilter,
  rootConceptIdsByColumn,
  groupedRows,
  groupedRowsDifferences,
}: Props) => {
  const applicableSecondaryIds = columnBuckets.secondaryIds.filter((column) =>
    exists(row[column.label]),
  );
  const secondaryIdsTooltip = applicableSecondaryIds
    .map((c) => c.label)
    .join(", ");

  const applicableConcepts = columnBuckets.concepts.filter((column) =>
    exists(row[column.label]),
  );
  const conceptsTooltip = applicableConcepts.map((c) => c.label).join(", ");

  const applicableMoney = columnBuckets.money.filter((column) =>
    exists(row[column.label]),
  );
  const moneyTooltip = applicableMoney.map((c) => c.label).join(", ");

  const applicableRest = columnBuckets.rest.filter((column) =>
    exists(row[column.label]),
  );
  const restTooltip = applicableRest.map((c) => c.label).join(", ");

  return (
    <Card>
      <Bullet />
      <RowDates dates={row.dates} />
      <SxRawDataBadge event={row} />
      <EventItemContent>
        <MainContent>
          {contentFilter.secondaryId && applicableSecondaryIds.length > 0 && (
            <>
              <WithTooltip text={secondaryIdsTooltip}>
                <SxFaIcon icon="microscope" active tiny />
              </WithTooltip>
              <ColBucket>
                {applicableSecondaryIds.map((column) => (
                  <div>
                    <TinyLabel>{column.label}</TinyLabel>
                    {row[column.label]}
                  </div>
                ))}
              </ColBucket>
            </>
          )}
          {contentFilter.money && applicableMoney.length > 0 && (
            <>
              <WithTooltip text={moneyTooltip}>
                <SxFaIcon icon="money-bill-alt" active tiny />
              </WithTooltip>
              <ColBucketCode>
                {applicableMoney.map((column) => (
                  <NumberFormat
                    {...currencyConfig}
                    displayType="text"
                    value={parseInt(row[column.label]) / 100}
                  />
                ))}
              </ColBucketCode>
            </>
          )}
          {contentFilter.concept && applicableConcepts.length > 0 && (
            <>
              <WithTooltip text={conceptsTooltip}>
                <SxFaIcon icon="folder" active tiny />
              </WithTooltip>
              <ColBucket>
                {applicableConcepts.map((column) => (
                  <ConceptName
                    rootConceptId={rootConceptIdsByColumn[column.label]}
                    conceptId={row[column.label]}
                    datasetId={datasetId}
                  />
                ))}
              </ColBucket>
            </>
          )}
          {contentFilter.rest && applicableRest.length > 0 && (
            <>
              <WithTooltip text={restTooltip}>
                <SxFaIcon icon="info" active tiny />
              </WithTooltip>
              <ColBucket>
                {applicableRest.map((column) => (
                  <span>{row[column.label]}</span>
                ))}
              </ColBucket>
            </>
          )}
        </MainContent>
        {groupedRowsDifferences && groupedRows && (
          <GroupedContent
            datasetId={datasetId}
            columns={columns}
            groupedRows={groupedRows}
            groupedRowsDifferences={groupedRowsDifferences}
            currencyConfig={currencyConfig}
            rootConceptIdsByColumn={rootConceptIdsByColumn}
          />
        )}
      </EventItemContent>
    </Card>
  );
};

export default EventCard;
