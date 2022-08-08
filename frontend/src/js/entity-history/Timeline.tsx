import styled from "@emotion/styled";
import { memo, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import {
  ColumnDescription,
  ColumnDescriptionSemanticConceptColumn,
  ConceptIdT,
  CurrencyConfigT,
} from "../api/types";
import type { StateT } from "../app/reducers";
import { useDatasetId } from "../dataset/selectors";
import { Heading4 } from "../headings/Headings";

import { ContentFilterValue } from "./ContentControl";
import type { DetailLevel } from "./DetailControl";
import type { EntityHistoryStateT, EntityEvent } from "./reducer";
import EventCard from "./timeline/EventCard";

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

    const rootConceptIdsByColumn: Record<string, ConceptIdT> = useMemo(() => {
      const entries: [string, ConceptIdT][] = [];

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

    const { bucketedEventsByDayAndSource } = useTimeBucketedSortedData(data, {
      sources,
    });

    console.log(bucketedEventsByDayAndSource);

    if (!datasetId) return null;

    return (
      <Root className={className}>
        {bucketedEventsByDayAndSource.map(({ year, quarterwiseData }) => {
          const totalEvents = quarterwiseData.reduce(
            (all, data) =>
              all + data.groupedEvents.reduce((s, evts) => s + evts.length, 0),
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
                {quarterwiseData.map(
                  ({ quarter, groupedEvents, differences }) => {
                    const totalEventsPerQuarter = groupedEvents.reduce(
                      (s, evts) => s + evts.length,
                      0,
                    );

                    return (
                      <QuarterGroup key={quarter}>
                        <QuarterHead empty={totalEventsPerQuarter === 0}>
                          <SxHeading4>
                            Q{quarter} – {totalEventsPerQuarter}{" "}
                            {t("history.events", {
                              count: totalEventsPerQuarter,
                            })}
                          </SxHeading4>
                          {detailLevel === "summary" && (
                            <Boxes>
                              {new Array(totalEventsPerQuarter)
                                .fill(0)
                                .map((_, i) => (
                                  <Box key={i} />
                                ))}
                            </Boxes>
                          )}
                        </QuarterHead>
                        {detailLevel !== "summary" &&
                          totalEventsPerQuarter > 0 && (
                            <EventTimeline>
                              <VerticalLine />
                              <EventItemList>
                                {groupedEvents.map((group, index) => {
                                  const groupDifferences = differences[index];

                                  if (group.length === 0) return null;

                                  if (
                                    detailLevel === "full" ||
                                    Object.keys(groupDifferences).length > 10
                                  ) {
                                    return group.map((evt, evtIdx) => (
                                      <EventCard
                                        key={`${index}-${evtIdx}`}
                                        columnBuckets={columnBuckets}
                                        datasetId={datasetId}
                                        contentFilter={contentFilter}
                                        rootConceptIdsByColumn={
                                          rootConceptIdsByColumn
                                        }
                                        row={evt}
                                        currencyConfig={currencyConfig}
                                      />
                                    ));
                                  } else {
                                    const firstRowWithoutDifferences =
                                      Object.fromEntries(
                                        Object.entries(group[0]).filter(
                                          ([k]) => {
                                            return !groupDifferences[k];
                                          },
                                        ),
                                      );

                                    return (
                                      <EventCard
                                        key={index}
                                        columnBuckets={columnBuckets}
                                        datasetId={datasetId}
                                        contentFilter={contentFilter}
                                        rootConceptIdsByColumn={
                                          rootConceptIdsByColumn
                                        }
                                        row={firstRowWithoutDifferences}
                                        currencyConfig={currencyConfig}
                                        eventGroupCount={group.length}
                                        differences={
                                          Object.keys(groupDifferences).length >
                                          0
                                            ? groupDifferences
                                            : undefined
                                        }
                                      />
                                    );
                                  }
                                })}
                              </EventItemList>
                            </EventTimeline>
                          )}
                      </QuarterGroup>
                    );
                  },
                )}
              </YearGroup>
            </>
          );
        })}
      </Root>
    );
  },
);

const diff = (objects: Object[]) => {
  if (objects.length < 2) return {};

  const differences: Record<string, Set<any>> = {};

  for (let i = 0; i < objects.length - 1; i++) {
    const o1 = objects[i] as any;
    const o2 = objects[i + 1] as any;
    const keys = Object.keys(o1);

    for (const key of keys) {
      if (
        o1.hasOwnProperty(key) &&
        o2.hasOwnProperty(key) &&
        JSON.stringify(o1[key]) !== JSON.stringify(o2[key])
      ) {
        if (differences[key]) {
          differences[key].add(o1[key]);
          differences[key].add(o2[key]);
        } else {
          differences[key] = new Set([o1[key], o2[key]]);
        }
      }
    }
  }

  return differences;
};

const findGroups = (eventsPerYears: EventsPerYear[]) => {
  return eventsPerYears.map(({ year, quarterwiseData }) => {
    return {
      year,
      quarterwiseData: quarterwiseData.map(({ quarter, events }) => {
        if (events.length < 2)
          return { quarter, groupedEvents: [events], differences: [{}] };

        const eventsByDayAndSource: EntityEvent[][] = [[events[0]]];

        for (let i = 1; i < events.length - 1; i++) {
          const evt = events[i];
          const lastEvt =
            eventsByDayAndSource[eventsByDayAndSource.length - 1][0];

          if (JSON.stringify(evt) === JSON.stringify(lastEvt)) {
            console.log(`DUPLICATE ${evt}`);
            continue;
          }

          if (
            JSON.stringify(evt.dates) === JSON.stringify(lastEvt.dates) &&
            evt.source === lastEvt.source
          ) {
            console.log("GROUP HIT");
            eventsByDayAndSource[eventsByDayAndSource.length - 1].push(evt);
          } else {
            eventsByDayAndSource.push([evt]);
          }
        }
        return {
          quarter,
          groupedEvents: eventsByDayAndSource,
          differences: eventsByDayAndSource.map((events) => diff(events)),
        };
      }),
    };
  });
};

interface EventsPerYear {
  year: number;
  quarterwiseData: {
    quarter: string;
    events: EntityEvent[];
  }[];
}

const useTimeBucketedSortedData = (
  data: EntityHistoryStateT["currentEntityData"],
  {
    sources,
  }: {
    sources: Set<string>;
  },
) => {
  const bucketEntityEvents = (
    entityData: EntityHistoryStateT["currentEntityData"],
    sources: Set<string>,
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

      if (sources.has(row.source)) {
        result[year][quarter].push(row);
      }
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
    const bucketedEvents = bucketEntityEvents(data, sources);
    const bucketedEventsByDayAndSource = findGroups(bucketedEvents);

    return {
      bucketedEvents,
      bucketedEventsByDayAndSource,
    };
  }, [data, sources]);
};
