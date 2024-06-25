import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import SearchBar from "../../search-bar/SearchBar";

import { clearSearch, useSearchItems } from "./actions";
import type { ProjectItemsSearchStateT } from "./reducer";

const ProjectItemsSearchBox = () => {
  const { t } = useTranslation();
  const search = useSelector<StateT, ProjectItemsSearchStateT>(
    (state) => state.projectItemsSearch,
  );

  const dispatch = useDispatch();
  const searchItems = useSearchItems();

  const onClear = useCallback(() => dispatch(clearSearch()), [dispatch]);

  return (
    <SearchBar
      searchTerm={search.searchTerm}
      placeholder={t("previousQueries.searchPlaceholder")}
      onClear={onClear}
      onSearch={searchItems}
    />
  );
};

export default ProjectItemsSearchBox;
