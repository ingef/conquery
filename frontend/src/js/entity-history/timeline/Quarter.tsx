import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  DatasetT,
} from "../../api/types";
import FaIcon from "../../icon/FaIcon";
import { ContentFilterValue } from "../ContentControl";
import { DetailLevel } from "../DetailControl";
import { ColumnBuckets } from "../Timeline";
import { EntityEvent } from "../reducer";

import EventCard from "./EventCard";
import { SmallHeading } from "./SmallHeading";

const EventTimeline = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
`;
const EventItemList = styled("div")`
  width: calc(100% + 10px);
  margin-left: -10px;
`;

const VerticalLine = styled("div")`
  height: calc(100% - 20px);
  width: 2px;
  background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
  margin: 10px 4px;
`;

const QuarterGroup = styled("div")``;
const QuarterHead = styled("div")<{ empty?: boolean }>`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme, empty }) =>
    empty ? theme.col.grayLight : theme.col.gray};
  display: grid;
  grid-template-columns: 20px 20px 100px 1fr;
  align-items: center;
  position: sticky;
  top: 0;
  z-index: 2;
  background-color: ${({ theme }) => theme.col.bgAlt};
  padding: 5px 0;
  cursor: pointer;
`;

const Boxes = styled("div")`
  display: flex;
  align-items: center;
`;
const Box = styled("div")`
  width: 2px;
  height: 14px;
  margin-left: 1px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
`;

const Quarter = ({
  quarter,
  year,
  totalEventsPerQuarter,
  isOpen,
  detailLevel,
  groupedEvents,
  toggleOpenQuarter,
  differences,
  columns,
  columnBuckets,
  currencyConfig,
  rootConceptIdsByColumn,
  contentFilter,
  datasetId,
}: {
  year: number;
  quarter: number;
  totalEventsPerQuarter: number;
  isOpen: boolean;
  groupedEvents: EntityEvent[][];
  detailLevel: DetailLevel;
  toggleOpenQuarter: (year: number, quarter: number) => void;
  differences: Record<string, Set<any>>[];
  datasetId: DatasetT["id"];
  columns: Record<string, ColumnDescription>;
  columnBuckets: ColumnBuckets;
  contentFilter: ContentFilterValue;
  currencyConfig: CurrencyConfigT;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
}) => {
  const { t } = useTranslation();

  return (
    <QuarterGroup key={quarter}>
      <QuarterHead
        empty={totalEventsPerQuarter === 0}
        onClick={() => toggleOpenQuarter(year, quarter)}
      >
        <FaIcon gray icon={isOpen ? "caret-down" : "caret-right"} />
        <SmallHeading>Q{quarter} </SmallHeading>
        <span>
          – {totalEventsPerQuarter}{" "}
          {t("history.events", {
            count: totalEventsPerQuarter,
          })}
        </span>
        {detailLevel === "summary" && (
          <Boxes>
            {new Array(totalEventsPerQuarter).fill(0).map((_, i) => (
              <Box key={i} />
            ))}
          </Boxes>
        )}
      </QuarterHead>
      {(isOpen || detailLevel !== "summary") && totalEventsPerQuarter > 0 && (
        <EventTimeline>
          <VerticalLine />
          <EventItemList>
            {groupedEvents.map((group, index) => {
              const groupDifferences = differences[index];

              if (group.length === 0) return null;

              if (detailLevel === "full") {
                return group.map((evt, evtIdx) => (
                  <EventCard
                    key={`${index}-${evtIdx}`}
                    columns={columns}
                    columnBuckets={columnBuckets}
                    datasetId={datasetId}
                    contentFilter={contentFilter}
                    rootConceptIdsByColumn={rootConceptIdsByColumn}
                    row={evt}
                    currencyConfig={currencyConfig}
                  />
                ));
              } else {
                const firstRowWithoutDifferences = Object.fromEntries(
                  Object.entries(group[0]).filter(([k]) => {
                    return !groupDifferences[k];
                  }),
                ) as EntityEvent;

                return (
                  <EventCard
                    key={index}
                    columns={columns}
                    columnBuckets={columnBuckets}
                    datasetId={datasetId}
                    contentFilter={contentFilter}
                    rootConceptIdsByColumn={rootConceptIdsByColumn}
                    row={firstRowWithoutDifferences}
                    currencyConfig={currencyConfig}
                    groupedRows={group}
                    groupedRowsDifferences={
                      Object.keys(groupDifferences).length > 0
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
};

export default memo(Quarter);
