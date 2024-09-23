import { useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import type { HistorySources, SelectOptionT } from "../api/types";
import type { StateT } from "../app/reducers";

export const useSourcesControl = () => {
  const [sourcesFilter, setSourcesFilter] = useState<SelectOptionT[]>([]);

  const sources = useSelector<StateT, HistorySources>(
    (state) => state.entityHistory.defaultParams.sources,
  );
  const allSourcesOptions = useMemo(
    () =>
      sources.all.map((s) => ({
        label: s.label,
        value: s.label, // Gotta use label since the value in the entity CSV is the source label as well
      })),
    [sources.all],
  );
  const defaultSourcesOptions = useMemo(
    () =>
      sources.default.map((s) => ({
        label: s.label,
        value: s.label, // Gotta use label since the value in the entity CSV is the source label as well
      })),
    [sources.default],
  );

  // TODO: Figure out whether we still need the current entity unique sources
  //
  // const currentEntityUniqueSources = useSelector<StateT, string[]>(
  //   (state) => state.entityHistory.currentEntityUniqueSources,
  // );
  // const currentEntitySourcesOptions = useMemo(
  //   () =>
  //     currentEntityUniqueSources.map((s) => ({
  //       label: s,
  //       value: s,
  //     })),
  //   [currentEntityUniqueSources],
  // );
  const sourcesSet = useMemo(
    () => new Set(sourcesFilter.map((o) => o.value as string)),
    [sourcesFilter],
  );

  useEffect(
    function takeDefaultIfEmpty() {
      setSourcesFilter((curr) =>
        curr.length === 0 ? defaultSourcesOptions : curr,
      );
    },
    [defaultSourcesOptions],
  );

  return {
    options: allSourcesOptions,
    sourcesSet,
    sourcesFilter,
    setSourcesFilter,
  };
};
