import styled from "@emotion/styled";
import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import { usePostPrefixForSuggestions } from "../api/api";
import { transformFilterValueToApi } from "../api/apiHelper";
import { ConceptT, RangeFilterT, TableT } from "../api/types";
import { StateT } from "../app/reducers";
import PrimaryButton from "../button/PrimaryButton";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import FaIcon from "../icon/FaIcon";
import { isMultiSelectFilter, resetFilters } from "../model/filter";
import { nodeIsElement } from "../model/node";
import TableFilters from "../query-node-editor/TableFilters";
import { filterSuggestionToSelectOption } from "../query-node-editor/suggestionsHelper";
import { FilterWithValueType } from "../standard-query-editor/types";
import { ModeT } from "../ui-components/InputRange";

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

const isRangeFilter = (filter: FilterWithValueType): filter is RangeFilterT =>
  filter.type === "REAL_RANGE" ||
  filter.type === "INTEGER_RANGE" ||
  filter.type === "MONEY_RANGE";

const useFilterState = (table: TableT) => {
  const [searchFilters, setSearchFilters] = useState<FilterWithValueType[]>(
    resetFilters(table.filters as FilterWithValueType[]),
  );

  const setFilterValue = useCallback((filterIdx: number, value: any) => {
    setSearchFilters((filters) =>
      filters.map((f, i) => (i === filterIdx ? { ...f, value } : f)),
    );
  }, []);

  const setFilterMode = useCallback((filterIdx: number, mode: ModeT) => {
    setSearchFilters((filters) =>
      filters.map((f, i) =>
        i === filterIdx && isRangeFilter(f) ? { ...f, mode } : f,
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
    setFilterMode,
    loadFilterSuggestions,
  };
};

const useSubmitSearch = ({
  searchFilters,
  onLoad,
}: {
  searchFilters: FilterWithValueType[];
  onLoad: (payload: LoadingPayload) => void;
}) => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const defaultStatusOptions = useDefaultStatusOptions();
  const onSubmitSearch = useCallback(async () => {
    setLoading(true);

    console.log(
      searchFilters.map((f) => ({
        filter: f.id,
        type: f.type,
        value: transformFilterValueToApi(f),
      })),
    );
    await new Promise((resolve) => setTimeout(resolve, 3000));

    setLoading(false);
    onLoad({
      label: t("history.searchResultLabel"),
      loadedEntityIds: [],
      loadedEntityStatus: {},
      loadedEntityStatusOptions: defaultStatusOptions,
    });
  }, [t, onLoad, defaultStatusOptions, searchFilters]);

  return {
    loading,
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

export const SearchEntitiesComponent = ({
  table,
  onLoad,
}: {
  table: TableT;
  onLoad: (payload: LoadingPayload) => void;
}) => {
  const {
    searchFilters,
    setFilterValue,
    setFilterMode,
    loadFilterSuggestions,
  } = useFilterState(table);

  const { loading, onSubmitSearch } = useSubmitSearch({
    searchFilters,
    onLoad,
  });

  return (
    <Root>
      <TableFilters
        filters={searchFilters}
        excludeTable={false}
        onSetFilterValue={setFilterValue}
        onSwitchFilterMode={setFilterMode}
        onLoadFilterSuggestions={loadFilterSuggestions}
      />
      <SxPrimaryButton onClick={onSubmitSearch} disabled={loading}>
        {loading && <FaIcon white icon="spinner" />}
        Submit
      </SxPrimaryButton>
    </Root>
  );
};
