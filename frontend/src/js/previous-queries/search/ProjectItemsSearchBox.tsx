import { FC, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import type { StateT } from "../../app/reducers";
import SearchBar from "../../search-bar/SearchBar";

import { clearSearch, useSearchItems } from "./actions";
import type { ProjectItemsSearchStateT } from "./reducer";

interface Props {
  className?: string;
}

const ProjectItemsSearchBox: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const search = useSelector<StateT, ProjectItemsSearchStateT>(
    (state) => state.projectItemsSearch,
  );

  const dispatch = useDispatch();
  const searchItems = useSearchItems();

  const onClear = useCallback(() => dispatch(clearSearch()), [dispatch]);

  return (
    <SearchBar
      className={className}
      searchTerm={search.searchTerm}
      placeholder={t("previousQueries.searchPlaceholder")}
      onClear={onClear}
      onSearch={searchItems}
    />
  );
};

export default ProjectItemsSearchBox;
