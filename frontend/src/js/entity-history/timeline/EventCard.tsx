import { css } from "@emotion/react";
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
import { RawDataBadge } from "./RawDataBadge";

const Card = styled("div")`
  display: grid;
  grid-template-columns: auto 45px 1fr;
  gap: 3px;
  font-size: ${({ theme }) => theme.font.xs};
  padding: 5px 0;
  position: relative;
`;

const EventItemContent = styled("div")<{ isGroup?: boolean }>`
  display: grid;
  grid-template-columns: auto 1fr;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 0 1px ${({ theme }) => theme.col.grayLight};
  padding: 15px 10px 5px;
  margin-top: 5px;
  gap: 5px;
  background-color: white;
  ${({ isGroup, theme }) =>
    isGroup &&
    css`
      border: 1px solid ${theme.col.blueGrayDark};
    `};
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
  top: -5px;
  left: -5px;
`;

const SxFaIcon = styled(FaIcon)`
  width: 20px !important;
  text-align: center;
`;

const ColBucketCode = styled((props: any) => (
  <ColBucket as="code" {...props} />
))``;

const TinyText = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.tiny};
  font-weight: 700;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.gray};
  line-height: 0.9;
`;

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
  columnBuckets: ColumnBuckets;
  datasetId: DatasetT["id"];
  currencyConfig: CurrencyConfigT;
  contentFilter: ContentFilterValue;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  differences?: Record<string, Set<any>>;
  eventGroupCount?: number;
}

const EventCard = ({
  row,
  columnBuckets,
  datasetId,
  currencyConfig,
  contentFilter,
  rootConceptIdsByColumn,
  differences,
  eventGroupCount,
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

  console.log(
    applicableConcepts,
    applicableMoney,
    applicableSecondaryIds,
    applicableRest,
  );

  return (
    <Card>
      <Bullet />
      <RowDates dates={row.dates} />
      <EventItemContent isGroup={!!differences}>
        <SxRawDataBadge event={row} />
        {differences && eventGroupCount && (
          <>
            <WithTooltip text="differences">
              <SxFaIcon icon="layer-group" active tiny />
            </WithTooltip>
            <div>
              <div>{eventGroupCount} events</div>
              {Object.entries(differences).map(([key, value]) => (
                <div>
                  <span>
                    {key}: {JSON.stringify([...value])}
                  </span>
                </div>
              ))}
            </div>
          </>
        )}
        {contentFilter.secondaryId && applicableSecondaryIds.length > 0 && (
          <>
            <WithTooltip text={secondaryIdsTooltip}>
              <SxFaIcon icon="microscope" active tiny />
            </WithTooltip>
            <ColBucket>
              {applicableSecondaryIds.map((column) => (
                <div>
                  <TinyText>{column.label}</TinyText>
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
                  column={column}
                  row={row}
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
      </EventItemContent>
    </Card>
  );
};

export default EventCard;
