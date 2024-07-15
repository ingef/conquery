import { useMemo } from "react";
import { ColumnDescription, ConceptIdT } from "../../../api/types";
import { EntityHistoryStateT } from "../../reducer";
import { useTimelineSearch } from "../../timeline-search/timelineSearchState";
import { findEventGroups } from "./findEventGroups";
import { groupByQuarter } from "./groupByQuarter";
import { ColumnBuckets } from "./useColumnInformation";

export const useTimeBucketedSortedData = (
  data: EntityHistoryStateT["currentEntityData"],
  {
    rootConceptIdsByColumn,
    columnBuckets,
    sources,
    secondaryIds,
    sourceColumn,
    dateColumn,
  }: {
    rootConceptIdsByColumn: Record<string, ConceptIdT>;
    columnBuckets: ColumnBuckets;
    sources: Set<string>;
    secondaryIds: ColumnDescription[];
    sourceColumn?: ColumnDescription;
    dateColumn?: ColumnDescription;
  },
) => {
  const { searchTerm } = useTimelineSearch();

  return useMemo(() => {
    if (!data || !dateColumn || !sourceColumn) {
      return {
        matches: 0,
        eventsByQuarterWithGroups: [],
      };
    }

    const eventsByQuarter = groupByQuarter(
      data,
      sources,
      dateColumn,
      sourceColumn,
      rootConceptIdsByColumn,
      columnBuckets,
      searchTerm,
    );

    const eventsByQuarterWithGroups = findEventGroups(
      eventsByQuarter,
      secondaryIds,
      dateColumn,
      sourceColumn,
    );

    const matches = searchTerm
      ? eventsByQuarterWithGroups
          .flatMap(({ quarterwiseData }) =>
            quarterwiseData.flatMap(({ groupedEvents }) => groupedEvents),
          )
          .reduce((acc, events) => acc + events.length, 0)
      : 0;

    return {
      matches,
      eventsByQuarterWithGroups,
    };
  }, [
    data,
    sources,
    secondaryIds,
    dateColumn,
    sourceColumn,
    columnBuckets,
    searchTerm,
    rootConceptIdsByColumn,
  ]);
};
