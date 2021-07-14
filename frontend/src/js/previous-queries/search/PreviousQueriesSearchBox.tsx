import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import SearchBar from "../../search-bar/SearchBar";

import { clearQueriesSearch, useSearchQueries } from "./actions";
import type { QueriesSearchStateT } from "./reducer";

interface Props {
  className?: string;
}

const PreviousQueriesSearchBox: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const search = useSelector<StateT, QueriesSearchStateT>(
    (state) => state.previousQueriesSearch,
  );

  const dispatch = useDispatch();
  const searchQueries = useSearchQueries();

  const onClear = () => dispatch(clearQueriesSearch());

  return (
    <SearchBar
      className={className}
      searchTerm={search.searchTerm}
      placeholder={t("previousQueries.searchPlaceholder")}
      onClear={onClear}
      onSearch={searchQueries}
    />
  );
};

export default PreviousQueriesSearchBox;
