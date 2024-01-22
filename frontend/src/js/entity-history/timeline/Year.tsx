import styled from "@emotion/styled";
import { memo } from "react";

import {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  TimeStratifiedInfo,
} from "../../api/types";
import { ContentFilterValue } from "../ContentControl";
import { DetailLevel } from "../DetailControl";
import { ColumnBuckets, EventsByQuarterWithGroups } from "../Timeline";

import Quarter from "./Quarter";
import YearHead from "./YearHead";

const YearGroup = styled("div")`
  display: flex;
  flex-direction: column;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const Year = ({
  year,
  getIsOpen,
  toggleOpenYear,
  toggleOpenQuarter,
  quarterwiseData,
  detailLevel,
  contentFilter,
  columns,
  dateColumn,
  sourceColumn,
  columnBuckets,
  currencyConfig,
  rootConceptIdsByColumn,
  timeStratifiedInfos,
}: {
  year: number;
  getIsOpen: (year: number, quarter?: number) => boolean;
  toggleOpenYear: (year: number) => void;
  toggleOpenQuarter: (year: number, quarter: number) => void;
  quarterwiseData: EventsByQuarterWithGroups[];
  detailLevel: DetailLevel;
  contentFilter: ContentFilterValue;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
  currencyConfig: CurrencyConfigT;
  columnBuckets: ColumnBuckets;
  columns: Record<string, ColumnDescription>;
  dateColumn: ColumnDescription;
  sourceColumn: ColumnDescription;
  timeStratifiedInfos: TimeStratifiedInfo[];
}) => {
  const isYearOpen = getIsOpen(year);
  const totalEvents = quarterwiseData.reduce(
    (all, data) =>
      all + data.groupedEvents.reduce((s, evts) => s + evts.length, 0),
    0,
  );

  return (
    <>
      <YearHead
        isOpen={isYearOpen}
        year={year}
        totalEvents={totalEvents}
        onClick={() => toggleOpenYear(year)}
        timeStratifiedInfos={timeStratifiedInfos}
      />
      <YearGroup key={year}>
        {quarterwiseData.map(({ quarter, groupedEvents, differences }) => {
          const totalEventsPerQuarter = groupedEvents.reduce(
            (s, evts) => s + evts.length,
            0,
          );
          const isQuarterOpen = getIsOpen(year, quarter);

          return (
            <Quarter
              key={quarter}
              isOpen={isYearOpen || isQuarterOpen}
              totalEventsPerQuarter={totalEventsPerQuarter}
              detailLevel={detailLevel}
              quarter={quarter}
              year={year}
              groupedEvents={groupedEvents}
              toggleOpenQuarter={toggleOpenQuarter}
              differences={differences}
              contentFilter={contentFilter}
              columns={columns}
              dateColumn={dateColumn}
              sourceColumn={sourceColumn}
              columnBuckets={columnBuckets}
              currencyConfig={currencyConfig}
              rootConceptIdsByColumn={rootConceptIdsByColumn}
            />
          );
        })}
      </YearGroup>
    </>
  );
};

export default memo(Year);
