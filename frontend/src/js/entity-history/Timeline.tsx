import { Fragment, memo } from "react";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type {
  CurrencyConfigT,
  EntityInfo,
  TimeStratifiedInfo,
} from "../api/types";
import type { StateT } from "../app/reducers";

import type { ContentFilterValue } from "./ContentControl";
import type { DetailLevel } from "./DetailControl";
import { EntityCard } from "./EntityCard";
import type { EntityHistoryStateT } from "./reducer";
import { TimelineEmptyPlaceholder } from "./timeline/TimelineEmptyPlaceholder";
import { useColumnInformation } from "./timeline/util/useColumnInformation";
import { useTimeBucketedSortedData } from "./timeline/util/useTimeBucketedSortedData";
import Year from "./timeline/Year";
import { TimelineSearch } from "./timeline-search/TimelineSearch";
import { useTimelineSearch } from "./timeline-search/timelineSearchState";

const root = tv({
  base: [
    "overflow-y-auto",
    "[-webkit-overflow-scrolling:touch]",
    "pt-0 pr-5 pb-5 pl-[10px]",
    "inline-grid",
    "grid-cols-[280px_auto]",
    "gap-x-[4px] gap-y-5",
    "w-full",
    "h-full",
  ],
  variants: {
    isEmpty: {
      true: "auto-rows-fr",
      false: "auto-rows-[minmax(min-content,max-content)_1fr]",
    },
  },
});

const divider = tv({
  base: ["col-start-1 col-span-2", "h-px", "bg-gray-100"],
});

export const Timeline = memo(
  ({
    className,
    currentEntityInfos,
    currentEntityTimeStratifiedInfos,
    detailLevel,
    sources,
    contentFilter,
    getIsOpen,
    toggleOpenYear,
    toggleOpenQuarter,
    blurred,
  }: {
    className?: string;
    currentEntityInfos: EntityInfo[];
    currentEntityTimeStratifiedInfos: TimeStratifiedInfo[];
    detailLevel: DetailLevel;
    sources: Set<string>;
    contentFilter: ContentFilterValue;
    getIsOpen: (year: number, quarter?: number) => boolean;
    toggleOpenYear: (year: number) => void;
    toggleOpenQuarter: (year: number, quarter: number) => void;
    blurred?: boolean;
  }) => {
    const data = useSelector<StateT, EntityHistoryStateT["currentEntityData"]>(
      (state) => state.entityHistory.currentEntityData,
    );
    const currencyConfig = useSelector<StateT, CurrencyConfigT>(
      (state) => state.startup.config.currency,
    );

    const { searchTerm } = useTimelineSearch();

    const {
      columns,
      dateColumn,
      sourceColumn,
      columnBuckets,
      rootConceptIdsByColumn,
    } = useColumnInformation();

    const { matches, eventsByQuarterWithGroups } = useTimeBucketedSortedData(
      data,
      {
        columnBuckets,
        rootConceptIdsByColumn,
        sourceColumn,
        dateColumn,
        sources,
        secondaryIds: columnBuckets.secondaryIds,
      },
    );

    const isEmpty =
      eventsByQuarterWithGroups.length === 0 || !dateColumn || !sourceColumn;

    return (
      <div className="overflow-hidden w-full flex flex-col">
        <TimelineSearch matches={matches} />
        <div className={root({ isEmpty, className })}>
          {!isEmpty && !searchTerm && (
            <EntityCard
              className="col-span-2"
              blurred={blurred}
              infos={currentEntityInfos}
              timeStratifiedInfos={currentEntityTimeStratifiedInfos}
            />
          )}
          {isEmpty && (
            <TimelineEmptyPlaceholder
              className="col-span-2 h-full"
              searchTerm={searchTerm}
            />
          )}
          {dateColumn &&
            sourceColumn &&
            eventsByQuarterWithGroups.map(({ year, quarterwiseData }, i) => (
              <Fragment key={year}>
                <Year
                  year={year}
                  quarterwiseData={quarterwiseData}
                  timeStratifiedInfos={currentEntityTimeStratifiedInfos}
                  getIsOpen={getIsOpen}
                  toggleOpenYear={toggleOpenYear}
                  toggleOpenQuarter={toggleOpenQuarter}
                  detailLevel={detailLevel}
                  currencyConfig={currencyConfig}
                  rootConceptIdsByColumn={rootConceptIdsByColumn}
                  columnBuckets={columnBuckets}
                  contentFilter={contentFilter}
                  columns={columns}
                  dateColumn={dateColumn}
                  sourceColumn={sourceColumn}
                />
                {i < eventsByQuarterWithGroups.length - 1 && (
                  <div className={divider()} />
                )}
              </Fragment>
            ))}
        </div>
      </div>
    );
  },
);
