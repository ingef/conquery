import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
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

const Grid = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 2px;
`;
const SxConceptName = styled(ConceptName)`
  grid-column: span 2;
`;

const ExtraArea = styled("div")`
  padding: 12px 34px;
  background-color: ${({ theme }) => theme.col.bg};
  display: flex;
  gap: 10px;
  border-top: 1px solid ${({ theme }) => theme.col.grayVeryLight};
`;

const ExtraAreaHeading = styled("h5")`
  font-size: ${({ theme }) => theme.font.xs};
  font-weight: 700;
  margin: 0 0 5px;
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
  currencyConfig: CurrencyConfigT;
  contentFilter: ContentFilterValue;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  differences?: Record<string, Set<any>>;
  eventGroupCount?: number;
}

const EventCard = ({
  row,
  columns,
  columnBuckets,
  datasetId,
  currencyConfig,
  contentFilter,
  rootConceptIdsByColumn,
  differences,
  eventGroupCount,
}: Props) => {
  const { t } = useTranslation();
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
        {differences && eventGroupCount && (
          <ExtraArea>
            <WithTooltip text={t("history.differencesTooltip")}>
              <SxFaIcon icon="layer-group" active tiny />
            </WithTooltip>
            <div>
              <ExtraAreaHeading>
                {eventGroupCount}{" "}
                {t("history.events", { count: eventGroupCount })}
              </ExtraAreaHeading>
              <Grid>
                {Object.entries(differences).map(([key, values]) => {
                  const columnsDescription = columns[key];
                  const isConceptColumn =
                    columnsDescription?.semantics.length > 0 &&
                    columnsDescription?.semantics[0].type === "CONCEPT_COLUMN";
                  const isMoneyColumn = columnsDescription.type === "MONEY";

                  if (isConceptColumn) {
                    return (
                      <>
                        {[...values].map((v) => (
                          <SxConceptName
                            rootConceptId={
                              rootConceptIdsByColumn[columnsDescription.label]
                            }
                            conceptId={v}
                            datasetId={datasetId}
                            title={columnsDescription.label}
                          />
                        ))}
                      </>
                    );
                  } else {
                    return (
                      <>
                        <div title={key}>{key}:</div>
                        <div
                          style={{
                            fontWeight: "400",
                            display: "flex",
                            gap: "10px",
                          }}
                        >
                          {isMoneyColumn
                            ? [...values].map((v) => (
                                <NumberFormat
                                  {...currencyConfig}
                                  displayType="text"
                                  value={parseInt(v) / 100}
                                />
                              ))
                            : [...values].map((v) => <span>{v}</span>)}
                        </div>
                      </>
                    );
                  }
                })}
              </Grid>
            </div>
          </ExtraArea>
        )}
      </EventItemContent>
    </Card>
  );
};

export default EventCard;
