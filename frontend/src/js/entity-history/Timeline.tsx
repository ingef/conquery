import styled from "@emotion/styled";
import { memo, useMemo } from "react";
import { useSelector } from "react-redux";

import {
  ColumnDescription,
  ColumnDescriptionSemanticConceptColumn,
  ConceptIdT,
  CurrencyConfigT,
} from "../api/types";
import type { StateT } from "../app/reducers";
import { useDatasetId } from "../dataset/selectors";

import { ContentFilterValue } from "./ContentControl";
import type { DetailLevel } from "./DetailControl";
import type { EntityHistoryStateT, EntityEvent } from "./reducer";
import Year from "./timeline/Year";
import {
  isConceptColumn,
  isMoneyColumn,
  isSecondaryIdColumn,
} from "./timeline/util";

const Root = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 20px 0 10px;
  display: inline-grid;
  grid-template-columns: 100px auto;
  grid-auto-rows: minmax(min-content, max-content);
  gap: 20px 4px;
  width: 100%;
`;

interface Props {
  className?: string;
  detailLevel: DetailLevel;
  sources: Set<string>;
  contentFilter: ContentFilterValue;
  getIsOpen: (year: number, quarter?: number) => boolean;
  toggleOpenYear: (year: number) => void;
  toggleOpenQuarter: (year: number, quarter: number) => void;
}

export const Timeline = ({
  className,
  detailLevel,
  sources,
  contentFilter,
  getIsOpen,
  toggleOpenYear,
  toggleOpenQuarter,
}: Props) => {
  const datasetId = useDatasetId();
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

  if (!datasetId) return null;

  return (
    <Root className={className}>
      {eventsByQuarterWithGroups.map(({ year, quarterwiseData }) => (
        <Year
          key={year}
          year={year}
          datasetId={datasetId}
          quarterwiseData={quarterwiseData}
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
      ))}
    </Root>
  );
};

export default memo(Timeline);

const diffObjects = (objects: Object[]) => {
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

const findGroupsWithinQuarter =
  (secondaryIds: ColumnDescription[]) =>
  ({ quarter, events }: { quarter: number; events: EntityEvent[] }) => {
    if (events.length < 2) {
      return { quarter, groupedEvents: [events], differences: [{}] };
    }

    const groupedEvents: EntityEvent[][] = [[events[0]]];

    for (let i = 1; i < events.length - 1; i++) {
      const evt = events[i];
      const lastEvt = groupedEvents[groupedEvents.length - 1][0];

      const isDuplicateEvent = JSON.stringify(evt) === JSON.stringify(lastEvt);

      if (isDuplicateEvent) {
        continue;
      }

      const datesMatch =
        evt.dates.from === lastEvt.dates.from &&
        evt.dates.to === lastEvt.dates.to;
      const sourcesMatch = evt.source === lastEvt.source;
      const groupableSecondaryIdsMatch = secondaryIds
        .filter(({ semantics }) => semantics.some((s) => s.type === "GROUP"))
        .every(({ label }) => evt[label] === lastEvt[label]);

      const similarEvents =
        datesMatch && sourcesMatch && groupableSecondaryIdsMatch;

      if (similarEvents) {
        groupedEvents[groupedEvents.length - 1].push(evt);
      } else {
        groupedEvents.push([evt]);
      }
    }
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
  differences: Record<string, Set<any>>[];
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
      .sort(([yearA], [yearB]) => {
        return parseInt(yearB) - parseInt(yearA);
      })
      .map(([year, quarterwiseData]) => ({
        year: parseInt(year),
        quarterwiseData: Object.entries(quarterwiseData)
          .sort(([quarterA], [quarterB]) => {
            return parseInt(quarterB) - parseInt(quarterA);
          })
          .map(([quarter, events]) => ({ quarter: parseInt(quarter), events })),
      }));

    // Fill empty years
    const currentYear = new Date().getFullYear();
    if (sortedEvents.length > 0 && sortedEvents[0].year < currentYear) {
      while (sortedEvents[0].year < currentYear) {
        sortedEvents.unshift({
          year: sortedEvents[0].year + 1,
          quarterwiseData: [1, 2, 3, 4].map((q) => ({
            quarter: q,
            events: [],
          })),
        });
      }
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
}

const useColumnInformation = () => {
  const columnDescriptions = useSelector<StateT, ColumnDescription[]>(
    (state) => state.entityHistory.columnDescriptions,
  );

  const columns = useSelector<StateT, EntityHistoryStateT["columns"]>(
    (state) => state.entityHistory.columns,
  );

  const columnBuckets: ColumnBuckets = useMemo(() => {
    return {
      money: columnDescriptions.filter(isMoneyColumn),
      concepts: columnDescriptions.filter(isConceptColumn),
      secondaryIds: columnDescriptions.filter(isSecondaryIdColumn),
      rest: columnDescriptions.filter(
        (c) => c.type !== "MONEY" && c.semantics.length === 0,
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
