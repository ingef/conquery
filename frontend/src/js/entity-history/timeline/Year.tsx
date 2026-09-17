import { memo } from "react";

import type {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
  TimeStratifiedInfo,
} from "../../api/types";
import type { ContentFilterValue } from "../ContentControl";
import type { DetailLevel } from "../DetailControl";
import { useTimelineSearch } from "../timeline-search/timelineSearchState";
import { Quarter } from "./Quarter";
import type { EventsByQuarterWithGroups } from "./util/findEventGroups";
import type { ColumnBuckets } from "./util/useColumnInformation";
import YearHead from "./YearHead";

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
  const { searchTerm } = useTimelineSearch();

  const isYearOpen = !!searchTerm || getIsOpen(year);
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
      <div className="flex flex-col rounded" key={year}>
        {quarterwiseData.map(({ quarter, groupedEvents, differences }) => {
          const totalEventsPerQuarter = groupedEvents.reduce(
            (s, evts) => s + evts.length,
            0,
          );
          const isQuarterOpen = !!searchTerm || getIsOpen(year, quarter);

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
      </div>
    </>
  );
};

export default memo(Year);
