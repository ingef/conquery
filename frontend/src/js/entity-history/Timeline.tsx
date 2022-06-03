import styled from "@emotion/styled";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import { Heading4 } from "../headings/Headings";
import WithTooltip from "../tooltip/WithTooltip";

import { DetailLevel } from "./DetailControl";
import { RowDates } from "./RowDates";
import type { EntityHistoryStateT, EntityEvent } from "./reducer";

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
  color: ${({ theme }) => theme.col.gray};
`;
const YearGroup = styled("div")`
  padding: 7px;
  box-shadow: 1px 1px 5px 1px rgba(0, 0, 0, 0.2);
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
`;
const QuarterGroup = styled("div")`
  padding-top: 7px;
  background-color: white;
`;
const QuarterHead = styled("div")`
  font-weight: 700;
  font-size: ${({ theme }) => theme.font.xs};
  color: black;
  display: grid;
  grid-template-columns: 120px 1fr;
`;

const SxHeading4 = styled(Heading4)`
  flex-shrink: 0;
  margin: 0;
`;
const SxWithTooltip = styled(WithTooltip)`
  color: black;
  flex-shrink: 0;
`;

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

interface Props {
  className?: string;
  data: EntityHistoryStateT["currentEntityData"];
  detailLevel: DetailLevel;
}

export const Timeline = ({ className, data, detailLevel }: Props) => {
  const { t } = useTranslation();

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
              return (
                <QuarterGroup key={quarter}>
                  <QuarterHead>
                    <span>
                      Q{quarter} – {events.length}{" "}
                      {t("history.events", { count: events.length })}
                    </span>
                    {detailLevel === "summary" && (
                      <Boxes>
                        {new Array(events.length).fill(0).map((_, i) => (
                          <Box key={i} />
                        ))}
                      </Boxes>
                    )}
                  </QuarterHead>
                  {detailLevel !== "summary" && (
                    <EventTimeline>
                      <VerticalLine />
                      <EventItemList>
                        {events
                          .slice(
                            0,
                            detailLevel === "detail" ? 3 : events.length,
                          )
                          .map((row, index) => {
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
                                  .slice(detailLevel === "full" ? 4 : 8)
                                  .reverse()
                                  .map((key, index, array) => (
                                    <SxWithTooltip
                                      key={key}
                                      place="top"
                                      text={
                                        detailLevel === "full" ? key : undefined
                                      }
                                    >
                                      <span style={{ flexShrink: 0 }}>
                                        {`${row[key]}${
                                          index !== array.length - 1
                                            ? " – "
                                            : ""
                                        }`}
                                      </span>
                                    </SxWithTooltip>
                                  ))}
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
