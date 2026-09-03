import { faCaretDown, faCaretRight } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type {
  ColumnDescription,
  ConceptIdT,
  CurrencyConfigT,
} from "../../api/types";
import FaIcon from "../../icon/FaIcon";
import type { ContentFilterValue } from "../ContentControl";
import type { DetailLevel } from "../DetailControl";
import type { EntityEvent } from "../reducer";
import EventCard from "./EventCard";
import { SmallHeading } from "./SmallHeading";
import type { ColumnBuckets } from "./util/useColumnInformation";

const eventTimeline = tv({
  base: ["grid grid-cols-[auto_1fr]"],
});

const eventItemList = tv({
  base: ["w-[calc(100%+10px)]", "-ml-[10px]"],
});

const verticalLine = tv({
  base: [
    "h-[calc(100%-20px)]",
    "w-[2px]",
    "bg-primary-50",
    "mx-[4px] my-[10px]",
  ],
});

const quarterHead = tv({
  base: [
    "sticky top-0",
    "z-2",
    "bg-bg-100",
    "-ml-[6px]",
    "w-[calc(100%+8px)]",
    "text-xs",
    "leading-none",
  ],
  variants: {
    empty: {
      true: "text-gray-100",
      false: "text-gray-500",
    },
  },
});

const inlineGrid = tv({
  base: [
    "inline-grid grid-cols-[20px_20px_110px_1fr] items-center",
    "cursor-pointer",
    "border border-transparent hover:border-primary-200",
    "rounded",
    "px-[10px] py-[6px]",
  ],
});

export const Quarter = memo(
  ({
    quarter,
    year,
    totalEventsPerQuarter,
    isOpen,
    detailLevel,
    groupedEvents,
    toggleOpenQuarter,
    differences,
    columns,
    dateColumn,
    sourceColumn,
    columnBuckets,
    currencyConfig,
    rootConceptIdsByColumn,
    contentFilter,
  }: {
    year: number;
    quarter: number;
    totalEventsPerQuarter: number;
    isOpen: boolean;
    groupedEvents: EntityEvent[][];
    detailLevel: DetailLevel;
    toggleOpenQuarter: (year: number, quarter: number) => void;
    differences: string[][];
    columns: Record<string, ColumnDescription>;
    dateColumn: ColumnDescription;
    sourceColumn: ColumnDescription;
    columnBuckets: ColumnBuckets;
    contentFilter: ContentFilterValue;
    currencyConfig: CurrencyConfigT;
    rootConceptIdsByColumn: Record<string, ConceptIdT>;
  }) => {
    const { t } = useTranslation();

    const areEventsShown =
      (isOpen || detailLevel !== "summary") && totalEventsPerQuarter > 0;

    return (
      <div key={quarter}>
        <div className={quarterHead({ empty: totalEventsPerQuarter === 0 })}>
          {/* biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a button */}
          {/* biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button */}
          <div
            className={inlineGrid()}
            onClick={() => toggleOpenQuarter(year, quarter)}
          >
            <FaIcon large gray icon={isOpen ? faCaretDown : faCaretRight} />
            <SmallHeading className="leading-none">Q{quarter} </SmallHeading>
            <span>
              – {totalEventsPerQuarter}{" "}
              {t("history.events", {
                count: totalEventsPerQuarter,
              })}
            </span>
            {detailLevel === "summary" && (
              <MemoizedBoxes totalEventsPerQuarter={totalEventsPerQuarter} />
            )}
          </div>
        </div>
        {areEventsShown && (
          <div className={eventTimeline()}>
            <div className={verticalLine()} />
            <div className={eventItemList()}>
              {groupedEvents.map((group, index) => {
                if (group.length === 0) return null;

                const groupDifferences = [
                  ...new Set([
                    ...differences[index],
                    ...columnBuckets.concepts
                      .filter((c) => !!group[0][c.label])
                      .map((c) => c.label),
                  ]),
                ];

                if (detailLevel === "full") {
                  return group.map((evt, evtIdx) => (
                    <EventCard
                      key={`${index}-${evtIdx}`}
                      columns={columns}
                      dateColumn={dateColumn}
                      sourceColumn={sourceColumn}
                      columnBuckets={columnBuckets}
                      contentFilter={contentFilter}
                      rootConceptIdsByColumn={rootConceptIdsByColumn}
                      row={evt}
                      currencyConfig={currencyConfig}
                    />
                  ));
                } else {
                  const firstRowWithoutDifferences = Object.fromEntries(
                    Object.entries(group[0]).filter(([key]) => {
                      if (key === dateColumn.label) {
                        return true; // always show dates, despite it being part of groupDifferences
                      }

                      return !groupDifferences.includes(key);
                    }),
                  ) as EntityEvent;

                  return (
                    <EventCard
                      key={index}
                      columns={columns}
                      dateColumn={dateColumn}
                      sourceColumn={sourceColumn}
                      columnBuckets={columnBuckets}
                      contentFilter={contentFilter}
                      rootConceptIdsByColumn={rootConceptIdsByColumn}
                      row={firstRowWithoutDifferences}
                      currencyConfig={currencyConfig}
                      groupedRows={group}
                      groupedRowsKeysWithDifferentValues={groupDifferences}
                    />
                  );
                }
              })}
            </div>
          </div>
        )}
      </div>
    );
  },
);

const MemoizedBoxes = memo(
  ({ totalEventsPerQuarter }: { totalEventsPerQuarter: number }) => {
    return (
      <div className="flex items-center">
        {new Array(totalEventsPerQuarter).fill(0).map((_, i) => (
          <div className="ml-px h-4 w-[2px] bg-primary-500" key={i} />
        ))}
      </div>
    );
  },
);
