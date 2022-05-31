import styled from "@emotion/styled";
import { useEffect, useMemo, useState } from "react";
import { ErrorBoundary } from "react-error-boundary";
import { useSelector } from "react-redux";

import { SelectOptionT } from "../api/types";
import { StateT } from "../app/reducers";
import { Heading4 } from "../headings/Headings";
import WithTooltip from "../tooltip/WithTooltip";

import { EntityHeader } from "./EntityHeader";
import { Navigation } from "./Navigation";
import { RowDates } from "./RowDates";
import { EntityEvent, EntityHistoryStateT } from "./reducer";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: ${({ theme }) => theme.col.bgAlt};
  padding: 60px 20px 20px;
  z-index: 2;
  display: grid;
  grid-template-columns: 200px 1fr;
`;

const Main = styled("div")`
  overflow: hidden;
  display: grid;
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
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: ${({ theme }) => theme.font.xs};
  padding: 3px 0;
  position: relative;
`;

const Bullet = styled("div")`
  width: 8px;
  height: 8px;
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

const Badge = styled("div")`
  border-radius: ${({ theme }) => theme.borderRadius};
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  padding: 1px 4px;
  font-size: ${({ theme }) => theme.font.xs};
  color: white;
  font-weight: 700;
`;

const YearHead = styled("div")`
  display: flex;
  gap: 5px;
  align-items: center;
  font-size: ${({ theme }) => theme.font.xs};
`;

const Timeline = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 10px;
  display: grid;
  gap: 10px;
`;
const YearGroup = styled("div")`
  padding: 7px;
  box-shadow: 1px 2px 3px 0px rgba(0, 0, 0, 0.2);
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
`;
const QuarterGroup = styled("div")`
  padding: 7px 4px;
  background-color: white;
`;
const QuarterHead = styled("p")`
  margin: 0;
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: black;
`;

const SxHeading4 = styled(Heading4)`
  flex-shrink: 0;
  margin: 0;
`;
const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
`;

export const History = () => {
  const [entityStatusOptions, setEntityStatusOptions] = useState<
    SelectOptionT[]
  >([]);
  const [entityIdsStatus, setEntityIdsStatus] = useState<{
    [id: string]: SelectOptionT[];
  }>({});

  const entityIds = useSelector<StateT, string[]>(
    (state) => state.entityHistory.entityIds,
  );

  const currentEntityId = useSelector<StateT, string | null>(
    (state) => state.entityHistory.currentEntityId,
  );
  const currentEntityData = useSelector<
    StateT,
    EntityHistoryStateT["currentEntityData"]
  >((state) => state.entityHistory.currentEntityData);

  const bucketedEntityDataByYearAndQuarter =
    useTimeBucketedDataDesc(currentEntityData);

  useEffect(() => {
    setEntityStatusOptions(
      [
        ...new Set(
          Object.values(entityIdsStatus).flatMap((opts) =>
            opts.map((o) => o.value as string),
          ),
        ),
      ].map((val) => ({ label: val, value: val })),
    );
  }, [entityIdsStatus]);

  return (
    <FullScreen>
      <Navigation
        entityIds={entityIds}
        entityIdsStatus={entityIdsStatus}
        currentEntityId={currentEntityId}
      />
      <Main>
        {currentEntityId && (
          <EntityHeader
            currentEntityId={currentEntityId}
            totalEvents={currentEntityData.length}
            entityIdsStatus={entityIdsStatus}
            entityStatusOptions={entityStatusOptions}
            setEntityIdsStatus={setEntityIdsStatus}
          />
        )}
        <ErrorBoundary
          fallbackRender={() => {
            return <div>Something went wrong here.</div>;
          }}
        >
          <Timeline>
            {bucketedEntityDataByYearAndQuarter.map(
              ({ year, quarterwiseData }) => {
                const totalEvents = quarterwiseData.reduce(
                  (all, data) => all + data.events.length,
                  0,
                );
                return (
                  <YearGroup key={year}>
                    <YearHead>
                      <SxHeading4>{year}</SxHeading4> – {totalEvents} events
                    </YearHead>
                    {quarterwiseData.map(({ quarter, events }) => {
                      return (
                        <QuarterGroup key={quarter}>
                          <QuarterHead>
                            Q{quarter} – {events.length} events
                          </QuarterHead>
                          <EventTimeline>
                            <VerticalLine />
                            <EventItemList>
                              {events.map((row, index) => {
                                return (
                                  <EventItem key={index}>
                                    <Bullet />
                                    <RowDates dates={row.dates} />
                                    <SxWithTooltip
                                      place="right"
                                      html={
                                        <pre
                                          style={{
                                            textAlign: "left",
                                            fontSize: "12px",
                                          }}
                                        >
                                          {JSON.stringify(row, null, 2)}
                                        </pre>
                                      }
                                    >
                                      <Badge
                                        style={{ cursor: "pointer" }}
                                        onClick={() => {
                                          if (navigator.clipboard) {
                                            navigator.clipboard.writeText(
                                              JSON.stringify(row, null, 2),
                                            );
                                          }
                                        }}
                                      >
                                        DATA
                                      </Badge>
                                    </SxWithTooltip>
                                    {Object.keys(row)
                                      .slice(4)
                                      .reverse()
                                      .map((key, index, array) => (
                                        <SxWithTooltip
                                          key={key}
                                          place="top"
                                          text={key}
                                        >
                                          {`${row[key]}${
                                            index !== array.length - 1
                                              ? " – "
                                              : ""
                                          }`}
                                        </SxWithTooltip>
                                      ))}
                                  </EventItem>
                                );
                              })}
                            </EventItemList>
                          </EventTimeline>
                        </QuarterGroup>
                      );
                    })}
                  </YearGroup>
                );
              },
            )}
          </Timeline>
        </ErrorBoundary>
      </Main>
    </FullScreen>
  );
};

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
