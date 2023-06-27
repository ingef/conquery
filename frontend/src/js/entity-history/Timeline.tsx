import styled from "@emotion/styled";
import { Fragment, memo, useMemo } from "react";
import { useSelector } from "react-redux";

import {
  ColumnDescription,
  ColumnDescriptionSemanticConceptColumn,
  ConceptIdT,
  CurrencyConfigT,
  EntityInfo,
  TimeStratifiedInfo,
} from "../api/types";
import type { StateT } from "../app/reducers";

import { ContentFilterValue } from "./ContentControl";
import type { DetailLevel } from "./DetailControl";
import { EntityCard } from "./EntityCard";
import type { EntityHistoryStateT, EntityEvent } from "./reducer";
import { TimelineEmptyPlaceholder } from "./timeline/TimelineEmptyPlaceholder";
import Year from "./timeline/Year";
import {
  isConceptColumn,
  isGroupableColumn,
  isIdColumn,
  isMoneyColumn,
  isSecondaryIdColumn,
  isVisibleColumn,
} from "./timeline/util";

const Root = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 20px 10px;
  display: inline-grid;
  grid-template-columns: 280px auto;
  grid-auto-rows: minmax(min-content, max-content);
  gap: 20px 4px;
  width: 100%;
`;

const Divider = styled("div")`
  grid-column: 1 / span 2;
  height: 1px;
  background: ${({ theme }) => theme.col.grayLight};
`;

const SxEntityCard = styled(EntityCard)`
  grid-column: span 2;
`;

const Timeline = ({
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

  const { columns, columnBuckets, rootConceptIdsByColumn } =
    useColumnInformation();

  const { eventsByQuarterWithGroups } = useTimeBucketedSortedData(data, {
    sources,
    secondaryIds: columnBuckets.secondaryIds,
  });

  if (eventsByQuarterWithGroups.length === 0) {
    return <TimelineEmptyPlaceholder />;
  }

  return (
    <Root className={className}>
      <SxEntityCard
        blurred={blurred}
        infos={currentEntityInfos}
        timeStratifiedInfos={currentEntityTimeStratifiedInfos}
      />
      {eventsByQuarterWithGroups.map(({ year, quarterwiseData }, i) => (
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
          />
          {i < eventsByQuarterWithGroups.length - 1 && <Divider />}
        </Fragment>
      ))}
    </Root>
  );
};

export default memo(Timeline);

const diffObjects = (objects: Object[]): string[] => {
  if (objects.length < 2) return [];

  const keysWithDifferentValues = new Set<string>();

  for (let i = 0; i < objects.length - 1; i++) {
    const o1 = objects[i] as any;
    const o2 = objects[i + 1] as any;
    const keys = Object.keys(o1); // Assumption: all objs have same keys

    for (const key of keys) {
      if (
        o1.hasOwnProperty(key) &&
        o2.hasOwnProperty(key) &&
        JSON.stringify(o1[key]) !== JSON.stringify(o2[key])
      ) {
        keysWithDifferentValues.add(key);
      }
    }
  }

  return [...keysWithDifferentValues];
};

const findGroupsWithinQuarter =
  (secondaryIds: ColumnDescription[]) =>
  ({ quarter, events }: { quarter: number; events: EntityEvent[] }) => {
    if (events.length < 2) {
      return { quarter, groupedEvents: [events], differences: [[]] };
    }

    const eventGroupBuckets: Record<string, EntityEvent[]> = {};

    for (let i = 0; i < events.length; i++) {
      const evt = events[i];
      const prevEvt = events[i - 1];
      const isDuplicateEvent =
        !!evt && !!prevEvt && JSON.stringify(evt) === JSON.stringify(prevEvt);

      if (isDuplicateEvent) {
        continue;
      }

      const groupKey =
        evt.source +
        secondaryIds
          .filter(isGroupableColumn)
          .map(({ label }) => evt[label])
          .join(",");

      if (eventGroupBuckets[groupKey]) {
        eventGroupBuckets[groupKey].push(evt);
      } else {
        eventGroupBuckets[groupKey] = [evt];
      }
    }

    const groupedEvents = Object.values(eventGroupBuckets).map((events) => {
      if (events.length > 0) {
        return [
          {
            ...events[0],
            dates: {
              from: events[0].dates.from,
              to: events[events.length - 1].dates.to,
            },
          },
          ...events.slice(1),
        ];
      }

      return events;
    });

    return {
      quarter,
      groupedEvents,
      differences: groupedEvents.map(diffObjects),
    };
  };

const findGroups = (
  eventsPerYears: EventsPerYear[],
  secondaryIds: ColumnDescription[],
) => {
  const findGroupsWithinYear = ({
    year,
    quarterwiseData,
  }: EventsPerYear): EventsByYearWithGroups => {
    return {
      year,
      quarterwiseData: quarterwiseData.map(
        findGroupsWithinQuarter(secondaryIds),
      ),
    };
  };

  return eventsPerYears.map(findGroupsWithinYear);
};

interface EventsPerYear {
  year: number;
  quarterwiseData: {
    quarter: number;
    events: EntityEvent[];
  }[];
}

interface EventsByYearWithGroups {
  year: number;
  quarterwiseData: EventsByQuarterWithGroups[];
}
export interface EventsByQuarterWithGroups {
  quarter: number;
  groupedEvents: EntityEvent[][];
  differences: string[][];
}

const useTimeBucketedSortedData = (
  data: EntityHistoryStateT["currentEntityData"],
  {
    sources,
    secondaryIds,
  }: {
    sources: Set<string>;
    secondaryIds: ColumnDescription[];
  },
) => {
  const groupByQuarter = (
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

    // Fill empty quarters
    for (const [, quarters] of Object.entries(result)) {
      for (const q of [1, 2, 3, 4]) {
        if (!quarters[q]) {
          quarters[q] = [];
        }
      }
    }

    const sortedEvents = Object.entries(result)
      .sort(([yearA], [yearB]) => parseInt(yearB) - parseInt(yearA))
      .map(([year, quarterwiseData]) => ({
        year: parseInt(year),
        quarterwiseData: Object.entries(quarterwiseData)
          .sort(([qA], [qB]) => parseInt(qB) - parseInt(qA))
          .map(([quarter, events]) => ({ quarter: parseInt(quarter), events })),
      }));

    if (sortedEvents.length === 0) {
      return sortedEvents;
    }

    // Fill empty years
    const currentYear = new Date().getFullYear();
    while (sortedEvents[0].year < currentYear) {
      sortedEvents.unshift({
        year: sortedEvents[0].year + 1,
        quarterwiseData: [4, 3, 2, 1].map((q) => ({
          quarter: q,
          events: [],
        })),
      });
    }

    return sortedEvents;
  };

  return useMemo(() => {
    const eventsByQuarter = groupByQuarter(data, sources);
    const eventsByQuarterWithGroups = findGroups(eventsByQuarter, secondaryIds);

    return {
      eventsByQuarterWithGroups,
    };
  }, [data, sources, secondaryIds]);
};

export interface ColumnBuckets {
  money: ColumnDescription[];
  concepts: ColumnDescription[];
  secondaryIds: ColumnDescription[];
  rest: ColumnDescription[];
  groupableIds: ColumnDescription[];
}

const useColumnInformation = () => {
  const columnDescriptions = useSelector<StateT, ColumnDescription[]>(
    (state) => state.entityHistory.columnDescriptions,
  );

  const columns = useSelector<StateT, EntityHistoryStateT["columns"]>(
    (state) => state.entityHistory.columns,
  );

  const columnBuckets: ColumnBuckets = useMemo(() => {
    const visibleColumnDescriptions =
      columnDescriptions.filter(isVisibleColumn);

    return {
      money: visibleColumnDescriptions.filter(isMoneyColumn),
      concepts: visibleColumnDescriptions.filter(isConceptColumn),
      secondaryIds: visibleColumnDescriptions.filter(isSecondaryIdColumn),
      groupableIds: visibleColumnDescriptions.filter(isGroupableColumn),
      rest: visibleColumnDescriptions.filter(
        (c) =>
          !isMoneyColumn(c) &&
          (c.semantics.length === 0 ||
            (!isGroupableColumn(c) && !isIdColumn(c))),
      ),
    };
  }, [columnDescriptions]);

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

  return {
    columns,
    columnBuckets,
    rootConceptIdsByColumn,
  };
};
