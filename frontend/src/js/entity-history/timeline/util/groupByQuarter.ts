import { ColumnDescription, ConceptIdT } from "../../../api/types";
import { getConceptById } from "../../../concept-trees/globalTreeStoreHelper";
import type { DateRow, EntityEvent, EntityHistoryStateT } from "../../reducer";
import { ColumnBuckets } from "./useColumnInformation";
import { isDateColumn, isSourceColumn } from "./util";

// Filter concepts by searchTerm
const isMatch = (str: string, searchTerm: string) =>
  str.toLowerCase().includes(searchTerm.toLowerCase());
const entryMatchesSearchTerm = ({
  entry: [key, value],
  columnBuckets,
  searchTerm,
  rootConceptIdsByColumn,
}: {
  entry: [key: string, value: unknown];
  columnBuckets: ColumnBuckets;
  searchTerm: string;
  rootConceptIdsByColumn: Record<string, ConceptIdT>;
}) => {
  const conceptColumn = columnBuckets.concepts.find((col) => col.label === key);

  if (conceptColumn) {
    const rootConceptId = rootConceptIdsByColumn[conceptColumn.label];
    const rootConcept = getConceptById(rootConceptId, rootConceptId);

    if (!rootConcept) return false;

    const concept = getConceptById(value as string, rootConceptId);

    if (!concept) return false;

    return isMatch(
      `${rootConcept.label} ${concept.label} - ${concept.description}`,
      searchTerm,
    );
  }

  const restColumn = columnBuckets.rest.find((col) => col.label === key);

  if (restColumn) {
    return isMatch(value as string, searchTerm);
  }

  const groupableColumn = columnBuckets.groupableIds.find(
    (col) =>
      col.label === key &&
      !isDateColumn(col) && // Because they're already displayed somewhere else
      !isSourceColumn(col),
  );

  if (groupableColumn) {
    return isMatch(value as string, searchTerm);
  }

  return false;
};

export const groupByQuarter = (
  entityData: EntityHistoryStateT["currentEntityData"],
  sources: Set<string>,
  dateColumn: ColumnDescription,
  sourceColumn: ColumnDescription,
  rootConceptIdsByColumn: Record<string, ConceptIdT>,
  columnBuckets: ColumnBuckets,
  searchTerm?: string,
) => {
  const result: { [year: string]: { [quarter: number]: EntityEvent[] } } = {};

  // Bucket by quarter
  for (const row of entityData) {
    const [year, month] = (row[dateColumn.label] as DateRow).from.split("-");
    const quarter = Math.floor((parseInt(month) - 1) / 3) + 1;

    if (!result[year]) {
      result[year] = { [quarter]: [] };
    } else if (!result[year][quarter]) {
      result[year][quarter] = [];
    }

    if (sources.has(row[sourceColumn.label] as string)) {
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

  // Sort within quarter
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

  const filteredSortedEvents = sortedEvents
    .map(({ year, quarterwiseData }) => ({
      year,
      quarterwiseData: !searchTerm
        ? quarterwiseData
        : quarterwiseData.map(({ quarter, events }) => ({
            quarter,
            events: events.filter((event) => {
              return Object.entries(event).some((entry) =>
                entryMatchesSearchTerm({
                  entry,
                  columnBuckets,
                  rootConceptIdsByColumn,
                  searchTerm,
                }),
              );
            }),
          })),
    }))
    .filter((year) =>
      !searchTerm
        ? year
        : year.quarterwiseData.some(({ events }) => events.length > 0),
    );

  return filteredSortedEvents;
};
