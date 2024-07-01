import { ColumnDescription } from "../../../api/types";
import { DateRow, EntityEvent } from "../../reducer";
import { isGroupableColumn } from "./util";

export interface EventsPerYear {
  year: number;
  quarterwiseData: {
    quarter: number;
    events: EntityEvent[];
  }[];
}

export interface EventsByYearWithGroups {
  year: number;
  quarterwiseData: EventsByQuarterWithGroups[];
}
export interface EventsByQuarterWithGroups {
  quarter: number;
  groupedEvents: EntityEvent[][];
  differences: string[][];
}

const diffObjects = (objects: object[]): string[] => {
  if (objects.length < 2) return [];

  const keysWithDifferentValues = new Set<string>();

  for (let i = 0; i < objects.length - 1; i++) {
    const o1 = objects[i];
    const o2 = objects[i + 1];
    const keys = Object.keys(o1); // Assumption: all objs have same keys

    for (const key of keys) {
      if (
        Object.prototype.hasOwnProperty.call(o1, key) &&
        Object.prototype.hasOwnProperty.call(o2, key) &&
        // @ts-ignore should be fine
        JSON.stringify(o1[key]) !== JSON.stringify(o2[key])
      ) {
        keysWithDifferentValues.add(key);
      }
    }
  }

  return [...keysWithDifferentValues];
};
const findGroupsWithinQuarter =
  (
    secondaryIds: ColumnDescription[],
    dateColumn: ColumnDescription,
    sourceColumn: ColumnDescription,
  ) =>
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
        evt[sourceColumn.label] +
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
            [dateColumn.label]: {
              from: (events[0][dateColumn.label] as DateRow).from,
              to: (events[events.length - 1][dateColumn.label] as DateRow).to,
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

export const findEventGroups = (
  eventsPerYears: EventsPerYear[],
  secondaryIds: ColumnDescription[],
  dateColumn: ColumnDescription,
  sourceColumn: ColumnDescription,
) => {
  const findGroupsWithinYear = ({
    year,
    quarterwiseData,
  }: EventsPerYear): EventsByYearWithGroups => {
    return {
      year,
      quarterwiseData: quarterwiseData.map(
        findGroupsWithinQuarter(secondaryIds, dateColumn, sourceColumn),
      ),
    };
  };

  return eventsPerYears.map(findGroupsWithinYear);
};
