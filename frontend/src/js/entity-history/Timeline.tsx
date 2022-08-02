import styled from "@emotion/styled";
import { memo, useMemo } from "react";
import { useTranslation } from "react-i18next";
import NumberFormat from "react-number-format";
import { useSelector } from "react-redux";

import {
  ColumnDescription,
  ColumnDescriptionSemanticConceptColumn,
  CurrencyConfigT,
} from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import { useDatasetId } from "../dataset/selectors";
import { Heading4 } from "../headings/Headings";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

import { ContentFilterValue } from "./ContentControl";
import type { DetailLevel } from "./DetailControl";
import { RowDates } from "./RowDates";
import type { EntityHistoryStateT, EntityEvent } from "./reducer";
import ConceptName from "./timeline/ConceptName";
import { RawDataBadge } from "./timeline/RawDataBadge";

const Root = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 5px 20px 5px 10px;
  display: inline-grid;
  grid-template-columns: 100px auto;
  grid-auto-rows: minmax(min-content, max-content);
  gap: 10px;
`;

const EventTimeline = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
`;
const EventItemList = styled("div")`
  width: 100%;
  margin-left: -10px;
`;

const EventItem = styled("div")`
  display: grid;
  grid-template-columns: auto 45px 1fr;
  gap: 3px;
  font-size: ${({ theme }) => theme.font.xs};
  padding: 5px 0;
  position: relative;
`;
const EventItemContent = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
  position: relative;
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 0 1px ${({ theme }) => theme.col.grayLight};
  padding: 15px 10px 5px;
  margin-top: 5px;
  gap: 5px;
  background-color: white;
`;

const Bullet = styled("div")`
  width: 10px;
  height: 10px;
  margin: 2px 0;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  flex-shrink: 0;
`;

const VerticalLine = styled("div")`
  height: calc(100% - 20px);
  width: 2px;
  background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
  margin: 10px 4px;
`;

const YearHead = styled("div")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
  padding: 10px;
`;
const StickyWrap = styled("div")`
  position: sticky;
  top: 0;
  left: 0;
`;
const YearGroup = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 7px;
  border-radius: ${({ theme }) => theme.borderRadius};
`;
const QuarterGroup = styled("div")``;
const QuarterHead = styled("div")<{ empty?: boolean }>`
  font-weight: ${({ empty }) => (empty ? "normal" : "bold")};
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, empty }) =>
    empty ? theme.col.grayLight : theme.col.gray};
  display: grid;
  grid-template-columns: 150px 1fr;
`;

const SxHeading4 = styled(Heading4)`
  flex-shrink: 0;
  margin: 0;
  color: ${({ theme }) => theme.col.black};
`;

const TinyText = styled("p")`
  margin: 0;
  font-size: ${({ theme }) => theme.font.tiny};
  font-weight: 700;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.gray};
  line-height: 0.9;
`;

const ColBucket = styled("div")`
  color: black;
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0 10px;
  padding: 1px 4px;
`;

const ColBucketCode = styled((props: any) => (
  <ColBucket as="code" {...props} />
))``;

const Boxes = styled("div")`
  display: flex;
  align-items: center;
`;
const Box = styled("div")`
  width: 3px;
  height: 8px;
  margin-left: 1px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
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

interface Props {
  className?: string;
  detailLevel: DetailLevel;
  sources: Set<string>;
  contentFilter: ContentFilterValue;
}

export const Timeline = memo(
  ({ className, detailLevel, sources, contentFilter }: Props) => {
    const { t } = useTranslation();
    const datasetId = useDatasetId();
    const data = useSelector<StateT, EntityHistoryStateT["currentEntityData"]>(
      (state) => state.entityHistory.currentEntityData,
    );
    const columns = useSelector<StateT, ColumnDescription[]>(
      (state) => state.entityHistory.columns,
    );

    const currencyConfig = useSelector<StateT, CurrencyConfigT>(
      (state) => state.startup.config.currency,
    );

    const columnBuckets = useMemo(() => {
      return {
        money: columns.filter((c) => c.type === "MONEY"),
        concepts: columns.filter(
          (c) =>
            c.semantics.length > 0 && c.semantics[0].type === "CONCEPT_COLUMN",
        ),
        secondaryIds: columns.filter(
          (c) =>
            c.semantics.length > 0 && c.semantics[0].type === "SECONDARY_ID",
        ),
        rest: columns.filter(
          (c) => c.type !== "MONEY" && c.semantics.length === 0,
        ),
      };
    }, [columns]);

    const rootConceptIdsByColumn = useMemo(() => {
      const entries = [];

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

    const bucketedEntityDataByYearAndQuarter = useTimeBucketedDataDesc(data);

    return (
      <Root className={className}>
        {bucketedEntityDataByYearAndQuarter.map(({ year, quarterwiseData }) => {
          const totalEvents = quarterwiseData.reduce(
            (all, data) => all + data.events.length,
            0,
          );
          return (
            <>
              <YearHead>
                <StickyWrap>
                  <SxHeading4>{year}</SxHeading4>
                  <div>
                    {totalEvents} {t("history.events", { count: totalEvents })}
                  </div>
                </StickyWrap>
              </YearHead>
              <YearGroup key={year}>
                {quarterwiseData.map(({ quarter, events }) => {
                  const filteredEvents = events.filter((e) =>
                    sources.has(e.source),
                  );

                  return (
                    <QuarterGroup key={quarter}>
                      <QuarterHead empty={filteredEvents.length === 0}>
                        <SxHeading4>
                          Q{quarter} – {filteredEvents.length}{" "}
                          {t("history.events", {
                            count: filteredEvents.length,
                          })}
                        </SxHeading4>
                        {detailLevel === "summary" && (
                          <Boxes>
                            {new Array(filteredEvents.length)
                              .fill(0)
                              .map((_, i) => (
                                <Box key={i} />
                              ))}
                          </Boxes>
                        )}
                      </QuarterHead>
                      {detailLevel !== "summary" && filteredEvents.length > 0 && (
                        <EventTimeline>
                          <VerticalLine />
                          <EventItemList>
                            {filteredEvents
                              .slice(
                                0,
                                detailLevel === "detail" ? 3 : events.length,
                              )
                              .map((row, index) => {
                                const applicableSecondaryIds =
                                  columnBuckets.secondaryIds.filter((column) =>
                                    exists(row[column.label]),
                                  );
                                const secondaryIdsTooltip =
                                  applicableSecondaryIds
                                    .map((c) => c.label)
                                    .join(", ");

                                const applicableConcepts =
                                  columnBuckets.concepts.filter((column) =>
                                    exists(row[column.label]),
                                  );
                                const conceptsTooltip = applicableConcepts
                                  .map((c) => c.label)
                                  .join(", ");

                                const applicableMoney =
                                  columnBuckets.money.filter((column) =>
                                    exists(row[column.label]),
                                  );
                                const moneyTooltip = applicableMoney
                                  .map((c) => c.label)
                                  .join(", ");

                                const applicableRest =
                                  columnBuckets.rest.filter((column) =>
                                    exists(row[column.label]),
                                  );
                                const restTooltip = applicableRest
                                  .map((c) => c.label)
                                  .join(", ");

                                return (
                                  <EventItem key={index}>
                                    <Bullet />
                                    <RowDates dates={row.dates} />
                                    <EventItemContent>
                                      <SxRawDataBadge event={row} />
                                      {contentFilter.secondaryId &&
                                        applicableSecondaryIds.length > 0 && (
                                          <>
                                            <WithTooltip
                                              text={secondaryIdsTooltip}
                                            >
                                              <SxFaIcon
                                                icon="microscope"
                                                active
                                                tiny
                                              />
                                            </WithTooltip>
                                            <ColBucket>
                                              {applicableSecondaryIds.map(
                                                (column) => (
                                                  <div>
                                                    <TinyText>
                                                      {column.label}
                                                    </TinyText>
                                                    {row[column.label]}
                                                  </div>
                                                ),
                                              )}
                                            </ColBucket>
                                          </>
                                        )}
                                      {contentFilter.money &&
                                        applicableMoney.length > 0 && (
                                          <>
                                            <WithTooltip text={moneyTooltip}>
                                              <SxFaIcon
                                                icon="money-bill-alt"
                                                active
                                                tiny
                                              />
                                            </WithTooltip>
                                            <ColBucketCode>
                                              {applicableMoney.map((column) => (
                                                <NumberFormat
                                                  {...currencyConfig}
                                                  displayType="text"
                                                  value={
                                                    parseInt(
                                                      row[column.label],
                                                    ) / 100
                                                  }
                                                />
                                              ))}
                                            </ColBucketCode>
                                          </>
                                        )}
                                      {contentFilter.concept &&
                                        applicableConcepts.length > 0 && (
                                          <>
                                            <WithTooltip text={conceptsTooltip}>
                                              <SxFaIcon
                                                icon="folder"
                                                active
                                                tiny
                                              />
                                            </WithTooltip>
                                            <ColBucket>
                                              {applicableConcepts.map(
                                                (column) => (
                                                  <ConceptName
                                                    rootConceptId={
                                                      rootConceptIdsByColumn[
                                                        column.label
                                                      ]
                                                    }
                                                    column={column}
                                                    row={row}
                                                    datasetId={datasetId}
                                                  />
                                                ),
                                              )}
                                            </ColBucket>
                                          </>
                                        )}
                                      {contentFilter.rest &&
                                        applicableRest.length > 0 && (
                                          <>
                                            <WithTooltip text={restTooltip}>
                                              <SxFaIcon
                                                icon="info"
                                                active
                                                tiny
                                              />
                                            </WithTooltip>
                                            <ColBucket>
                                              {applicableRest.map((column) => (
                                                <span>{row[column.label]}</span>
                                              ))}
                                            </ColBucket>
                                          </>
                                        )}
                                    </EventItemContent>
                                  </EventItem>
                                );
                              })}
                          </EventItemList>
                        </EventTimeline>
                      )}
                    </QuarterGroup>
                  );
                })}
              </YearGroup>
            </>
          );
        })}
      </Root>
    );
  },
);

const useTimeBucketedDataDesc = (
  data: EntityHistoryStateT["currentEntityData"],
): {
  year: number;
  quarterwiseData: { quarter: string; events: EntityEvent[] }[];
}[] => {
  const bucketEntityEvents = (
    entityData: EntityHistoryStateT["currentEntityData"],
  ) => {
    const result: { [year: string]: { [quarter: number]: EntityEvent[] } } = {};

    for (const row of entityData) {
      const [year, month] = row.dates.from.split("-");
      const quarter = Math.floor((parseInt(month) - 1) / 3) + 1;

      if (!result[year]) {
        result[year] = { [quarter]: [] };
      } else if (!result[year][quarter]) {
        result[year][quarter] = [];
      }

      result[year][quarter].push(row);
    }

    return Object.entries(result)
      .sort(([yearA], [yearB]) => {
        return parseInt(yearB) - parseInt(yearA);
      })
      .map(([year, quarterwiseData]) => ({
        year: parseInt(year),
        quarterwiseData: Object.entries(quarterwiseData)
          .sort(([quarterA], [quarterB]) => {
            return parseInt(quarterB) - parseInt(quarterA);
          })
          .map(([quarter, events]) => ({ quarter, events })),
      }));
  };

  return useMemo(() => {
    return bucketEntityEvents(data);
  }, [data]);
};
