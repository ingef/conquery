import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import {
  usePostPrefixForSuggestions,
  usePostResolveEntities,
} from "../api/api";
import { transformFilterValueToApi } from "../api/apiHelper";
import type { TableT } from "../api/types";
import type { StateT } from "../app/reducers";
import PrimaryButton from "../button/PrimaryButton";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { useDatasetId } from "../dataset/selectors";
import { isMultiSelectFilter, resetFilters } from "../model/filter";
import { nodeIsElement } from "../model/node";
import { filterSuggestionToSelectOption } from "../query-node-editor/suggestionsHelper";
import TableFilters from "../query-node-editor/TableFilters";
import type {
  BigMultiSelectFilterWithValueType,
  MultiSelectFilterWithValueType,
} from "../standard-query-editor/types";
import { Icon } from "../ui-components/Icon";

import type { LoadingPayload } from "./LoadHistoryDropzone";
import { useDefaultStatusOptions } from "./useDefaultStatusOptions";

export const SearchEntites = ({
  onLoad,
}: {
  onLoad: (payload: LoadingPayload) => void;
}) => {
  const searchConceptTable = useSelector<StateT, TableT | undefined>(
    (state) => {
      const { searchConcept, searchConnector } =
        state.entityHistory.defaultParams;
      const concept = searchConcept ? getConceptById(searchConcept) : undefined;

      if (!concept || !nodeIsElement(concept)) return undefined;

      return concept.tables?.find((t) => t.connectorId === searchConnector);
    },
  );

  if (!searchConceptTable) return null;

  return <SearchEntitiesComponent table={searchConceptTable} onLoad={onLoad} />;
};

type MultiSelectFilter =
  | MultiSelectFilterWithValueType
  | BigMultiSelectFilterWithValueType;

const useFilterState = (table: TableT) => {
  const allowlistedSearchFilters = useSelector<StateT, string[]>(
    (state) => state.entityHistory.defaultParams.searchFilters,
  );
  const [searchFilters, setSearchFilters] = useState<MultiSelectFilter[]>(
    resetFilters(
      table.filters
        .filter((f) => allowlistedSearchFilters.includes(f.id))
        .filter(
          (f): f is MultiSelectFilter =>
            f.type === "BIG_MULTI_SELECT" || f.type === "MULTI_SELECT",
        ),
    ) as MultiSelectFilter[],
  );

  const setFilterValue = useCallback((filterIdx: number, value: unknown) => {
    setSearchFilters((filters) =>
      filters.map((f, i) =>
        i === filterIdx
          ? { ...f, value: value as MultiSelectFilterWithValueType["value"] }
          : f,
      ),
    );
  }, []);

  const postPrefixForSuggestions = usePostPrefixForSuggestions();

  const loadFilterSuggestions = useCallback(
    async (
      _: number,
      filterId: string,
      prefix: string,
      page: number,
      pageSize: number,
    ) => {
      const filter = searchFilters.find((f) => f.id === filterId);
      if (!filter || !isMultiSelectFilter(filter)) {
        return null;
      }

      const suggestions = await postPrefixForSuggestions({
        filterId,
        prefix,
        page,
        pageSize,
      });

      const nextOptions =
        page === 0
          ? suggestions.values.map(filterSuggestionToSelectOption)
          : [
              ...filter.options,
              ...suggestions.values
                .filter((v) => !filter.options.find((o) => o.value === v.value))
                .map(filterSuggestionToSelectOption),
            ];

      const filterParams = {
        options: nextOptions,
        total: suggestions.total,
      };

      setSearchFilters((filters) =>
        filters.map((f) =>
          f.id === filterId && isMultiSelectFilter(f)
            ? { ...f, ...filterParams }
            : f,
        ),
      );

      return suggestions;
    },
    [searchFilters, postPrefixForSuggestions],
  );

  return {
    searchFilters,
    setFilterValue,
    loadFilterSuggestions,
  };
};

const useSubmitSearch = ({
  searchFilters,
  onLoad,
}: {
  searchFilters: (
    | MultiSelectFilterWithValueType
    | BigMultiSelectFilterWithValueType
  )[];
  onLoad: (payload: LoadingPayload) => void;
}) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const defaultStatusOptions = useDefaultStatusOptions();
  const datasetId = useDatasetId();
  const postResolveEntities = usePostResolveEntities();

  const onSubmitSearch = useCallback(async () => {
    if (!datasetId) return;

    setLoading(true);

    const filterValues = searchFilters
      .map((f) => ({
        filter: f.id,
        type: f.type as "MULTI_SELECT" | "BIG_MULTI_SELECT",
        value: transformFilterValueToApi(f) as string[],
      }))
      .filter((f) => f.value.length !== 0);

    try {
      const result = await postResolveEntities(datasetId, filterValues);

      const loadedEntityIds = result.map((e) => {
        const keys = Object.keys(e);
        return { id: e[keys[0]], kind: keys[0] };
      });

      setLoading(false);
      onLoad({
        label: t("history.searchResultLabel"),
        loadedEntityIds,
        loadedEntityStatus: {},
        loadedEntityStatusOptions: defaultStatusOptions,
      });
    } catch (e) {
      setLoading(false);
      throw e;
    }
  }, [
    t,
    datasetId,
    onLoad,
    defaultStatusOptions,
    searchFilters,
    postResolveEntities,
  ]);

  const hasFiltersSet = useMemo(
    () => searchFilters.some((f) => (f.value?.length ?? 0) > 0),
    [searchFilters],
  );

  return {
    loading,
    hasFiltersSet,
    onSubmitSearch,
  };
};

const root = tv({
  base: [
    "flex flex-col items-center",
    "gap-[14px]",
    "px-3 py-[10px]",
    "[&>div]:w-full",
  ],
});

const submitButton = tv({
  base: ["flex items-center justify-center", "w-full", "gap-[14px]"],
});

const noop = () => {};

export const SearchEntitiesComponent = ({
  table,
  onLoad,
}: {
  table: TableT;
  onLoad: (payload: LoadingPayload) => void;
}) => {
  const { t } = useTranslation();
  const { searchFilters, setFilterValue, loadFilterSuggestions } =
    useFilterState(table);

  const { loading, hasFiltersSet, onSubmitSearch } = useSubmitSearch({
    searchFilters,
    onLoad,
  });

  if (searchFilters.length === 0) return null;

  return (
    <div className={root()}>
      <TableFilters
        filters={searchFilters}
        excludeTable={false}
        onSetFilterValue={setFilterValue}
        onSwitchFilterMode={noop}
        onLoadFilterSuggestions={loadFilterSuggestions}
      />
      <PrimaryButton
        className={submitButton()}
        onClick={onSubmitSearch}
        disabled={!hasFiltersSet || loading}
      >
        {loading && <Icon icon={faSpinner} className="text-white" />}
        {t("history.searchEntitiesButton")}
      </PrimaryButton>
    </div>
  );
};
