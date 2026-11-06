import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import { SearchField } from "../../ui-components/SearchField";

import { clearSearch, useSearchItems } from "./actions";
import type { ProjectItemsSearchStateT } from "./reducer";

const ProjectItemsSearchBox = ({ className }: { className?: string }) => {
  const { t } = useTranslation();
  const search = useSelector<StateT, ProjectItemsSearchStateT>(
    (state) => state.projectItemsSearch,
  );

  const dispatch = useDispatch();
  const searchItems = useSearchItems();

  const onClear = useCallback(() => dispatch(clearSearch()), [dispatch]);

  const [term, setTerm] = useState(search.searchTerm ?? "");
  useEffect(() => {
    setTerm(search.searchTerm ?? "");
  }, [search.searchTerm]);

  const placeholder = t("previousQueries.searchPlaceholder");

  return (
    <div className={className}>
      <SearchField
        aria-label={placeholder}
        placeholder={placeholder}
        value={term}
        onChange={(value) => {
          setTerm(value);
          if (!value) onClear();
        }}
        onSubmit={searchItems}
        onClear={onClear}
      />
    </div>
  );
};

export default ProjectItemsSearchBox;
