import styled from "@emotion/styled";
import { memo, useMemo } from "react";
import { useTranslation } from "react-i18next";
import NumberFormat from "react-number-format";
import { useSelector } from "react-redux";

import { ColumnDescription, CurrencyConfigT } from "../api/types";
import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";
import { Heading4 } from "../headings/Headings";
import FaIcon from "../icon/FaIcon";
import WithTooltip from "../tooltip/WithTooltip";

import { ContentFilterValue } from "./ContentControl";
import type { DetailLevel } from "./DetailControl";
import { RowDates } from "./RowDates";
import type { EntityHistoryStateT, EntityEvent } from "./reducer";
import { RawDataBadge } from "./timeline/RawDataBadge";

const Root = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 5px 20px 5px 10px;
  display: flex;
  flex-direction: column;
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
  grid-template-columns: auto 60px 1fr;
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
  gap: 5px;
`;

const Bullet = styled("div")`
  width: 8px;
  height: 8px;
  margin: 4px 0;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  flex-shrink: 0;
`;

const VerticalLine = styled("div")`
  height: calc(100% - 20px);
  width: 2px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  margin: 10px 5px;
`;

const YearHead = styled("div")`
  display: flex;
  gap: 5px;
  align-items: center;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
`;
const YearGroup = styled("div")`
  padding: 7px;
  box-shadow: 1px 1px 3px 0px rgba(0, 0, 0, 0.2);
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
`;
const QuarterGroup = styled("div")`
  padding-top: 7px;
  background-color: white;
`;
const QuarterHead = styled("div")<{ empty?: boolean }>`
  font-weight: ${({ empty }) => (empty ? "normal" : "bold")};
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, empty }) => (empty ? theme.col.gray : theme.col.black)};
  display: grid;
  grid-template-columns: 120px 1fr;
`;

const SxHeading4 = styled(Heading4)`
  flex-shrink: 0;
  margin: 0;
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
  background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
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
    const data = useSelector<StateT, EntityHistoryStateT["currentEntityData"]>(
      (state) => state.entityHistory.currentEntityData,
    );
    const columns = useSelector<StateT, ColumnDescription[]>(
      (state) => state.entityHistory.columns,
    );

    console.log(columns);

    const currencyConfig = useSelector<StateT, CurrencyConfigT>(
      (state) => state.startup.config.currency,
    );

    const columnBuckets = useMemo(() => {
      return {
        ids: columns.filter(
          (c) => c.semantics.length > 0 && c.semantics[0].type === "ID",
        ),
        secondaryIds: columns.filter(
          (c) =>
            c.semantics.length > 0 && c.semantics[0].type === "SECONDARY_ID",
        ),
        concepts: columns.filter(
          (c) =>
            c.semantics.length > 0 && c.semantics[0].type === "CONCEPT_COLUMN",
        ),
        money: columns.filter((c) => c.type === "MONEY"),
        rest: columns.filter(
          (c) => c.type !== "MONEY" && c.semantics.length === 0,
        ),
      };
    }, [columns]);

    const bucketedEntityDataByYearAndQuarter = useTimeBucketedDataDesc(data);

    return (
      <Root className={className}>
        {bucketedEntityDataByYearAndQuarter.map(({ year, quarterwiseData }) => {
          const totalEvents = quarterwiseData.reduce(
            (all, data) => all + data.events.length,
            0,
          );
          return (
            <YearGroup key={year}>
              <YearHead>
                <SxHeading4>{year}</SxHeading4> – {totalEvents}{" "}
                {t("history.events", { count: totalEvents })}
              </YearHead>
              {quarterwiseData.map(({ quarter, events }) => {
                const filteredEvents = events.filter((e) =>
                  sources.has(e.source),
                );

                return (
                  <QuarterGroup key={quarter}>
                    <QuarterHead empty={filteredEvents.length === 0}>
                      <span>
                        Q{quarter} – {filteredEvents.length}{" "}
                        {t("history.events", { count: filteredEvents.length })}
                      </span>
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
                              const secondaryIdsTooltip = applicableSecondaryIds
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

                              const applicableRest = columnBuckets.rest.filter(
                                (column) => exists(row[column.label]),
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
                                                  parseInt(row[column.label]) /
                                                  100
                                                }
                                              />
                                            ))}
                                          </ColBucketCode>
                                        </>
                                      )}
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
                                                <span>{row[column.label]}</span>
                                              ),
                                            )}
                                          </ColBucket>
                                        </>
                                      )}
                                    {contentFilter.rest &&
                                      applicableRest.length > 0 && (
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
