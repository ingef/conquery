import styled from "@emotion/styled";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import {
  usePostPrefixForSuggestions,
  usePostResolveEntities,
} from "../api/api";
import { transformFilterValueToApi } from "../api/apiHelper";
import { ConceptT, TableT } from "../api/types";
import { StateT } from "../app/reducers";
import PrimaryButton from "../button/PrimaryButton";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { useDatasetId } from "../dataset/selectors";
import FaIcon from "../icon/FaIcon";
import { isMultiSelectFilter, resetFilters } from "../model/filter";
import { nodeIsElement } from "../model/node";
import TableFilters from "../query-node-editor/TableFilters";
import { filterSuggestionToSelectOption } from "../query-node-editor/suggestionsHelper";
import {
  BigMultiSelectFilterWithValueType,
  MultiSelectFilterWithValueType,
} from "../standard-query-editor/types";

import { useDefaultStatusOptions } from "./History";
import { LoadingPayload } from "./LoadHistoryDropzone";

export const SearchEntites = ({
  onLoad,
}: {
  onLoad: (payload: LoadingPayload) => void;
}) => {
  const searchConcept = useSelector<StateT, ConceptT | null>((state) =>
    state.entityHistory.defaultParams.searchConcept
      ? getConceptById(state.entityHistory.defaultParams.searchConcept)
      : null,
  );

  if (
    !searchConcept ||
    !nodeIsElement(searchConcept) ||
    searchConcept.tables?.length !== 1
  ) {
    return null;
  }

  const searchConceptTable = searchConcept.tables[0];

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

  const setFilterValue = useCallback((filterIdx: number, value: any) => {
    setSearchFilters((filters) =>
      filters.map((f, i) => (i === filterIdx ? { ...f, value } : f)),
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

const Root = styled("div")`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 10px 12px;

  > div {
    width: 100%;
  }
`;

const SxPrimaryButton = styled(PrimaryButton)`
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
`;

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
    <Root>
      <TableFilters
        filters={searchFilters}
        excludeTable={false}
        onSetFilterValue={setFilterValue}
        onSwitchFilterMode={noop}
        onLoadFilterSuggestions={loadFilterSuggestions}
      />
      <SxPrimaryButton
        onClick={onSubmitSearch}
        disabled={!hasFiltersSet || loading}
      >
        {loading && <FaIcon white icon={faSpinner} />}
        {t("history.searchEntitiesButton")}
      </SxPrimaryButton>
    </Root>
  );
};
