import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import ConceptTreesOpenButtons from "../concept-trees-open/ConceptTreesOpenButtons";
import AnimatedDots from "../ui-components/AnimatedDots";
import { Button } from "../ui-components/Button";
import { SearchField } from "../ui-components/SearchField";

import {
  clearSearchQuery,
  toggleShowMismatches,
  useSearchTrees,
} from "./actions";
import type { SearchT, TreesT } from "./reducer";

const root = tv({ base: "relative" });

const tinyText = tv({
  base: ["my-[3px]", "text-xs", "text-gray-500"],
});

const row = tv({
  base: "flex flex-row items-center justify-between",
});

const displaying = tv({
  base: ["text-xs", "uppercase", "text-gray-500"],
});

const ConceptTreeSearchBox = ({ className }: { className?: string }) => {
  const showMismatches = useSelector<StateT, boolean>(
    (state) => state.conceptTrees.search.showMismatches,
  );
  const search = useSelector<StateT, SearchT>(
    (state) => state.conceptTrees.search,
  );
  const trees = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );
  const treesRef = useRef(trees);
  treesRef.current = trees;

  const dispatch = useDispatch();
  const { t } = useTranslation();

  const searchTrees = useSearchTrees();
  const onSearch = useCallback(
    (searchString: string) => {
      if (searchString.length > 1) {
        searchTrees(treesRef.current, searchString);
      }
    },
    [searchTrees],
  );
  const onClearQuery = useCallback(
    () => dispatch(clearSearchQuery()),
    [dispatch],
  );
  const onToggleShowMismatches = () => dispatch(toggleShowMismatches());

  const [term, setTerm] = useState(search.query ?? "");
  useEffect(() => {
    setTerm(search.query ?? "");
  }, [search.query]);

  const placeholder = t("conceptTreeList.searchPlaceholder");

  return (
    <div className={root({ className })}>
      <div className="flex items-center gap-[5px]">
        <ConceptTreesOpenButtons />
        <div className="grow">
          <SearchField
            aria-label={placeholder}
            placeholder={placeholder}
            value={term}
            onChange={(value) => {
              setTerm(value);
              if (!value) onClearQuery();
            }}
            onSubmit={onSearch}
            onClear={onClearQuery}
          />
        </div>
      </div>
      {search.loading ? (
        <AnimatedDots />
      ) : (
        search.result &&
        search.resultCount >= 0 && (
          <div className={row()}>
            <p className={tinyText()}>
              {t("search.resultLabel", {
                totalResults: search.resultCount,
                duration: (search.duration / 1000.0).toFixed(2),
              })}
            </p>
            <div className="my-[3px] flex items-center gap-[5px]">
              <span className={displaying()}>
                {showMismatches
                  ? t("conceptTreeList.showingMismatches")
                  : t("conceptTreeList.showingMatchesOnly")}
              </span>
              <Button
                intent="secondary"
                size="sm"
                onPress={onToggleShowMismatches}
              >
                {showMismatches
                  ? t("conceptTreeList.showMatchesOnly")
                  : t("conceptTreeList.showMismatches")}
              </Button>
            </div>
          </div>
        )
      )}
    </div>
  );
};

export default ConceptTreeSearchBox;
