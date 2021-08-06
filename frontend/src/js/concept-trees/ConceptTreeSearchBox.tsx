import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import TransparentButton from "../button/TransparentButton";
import AnimatedDots from "../common/components/AnimatedDots";
import ConceptTreesOpenButtons from "../concept-trees-open/ConceptTreesOpenButtons";
import SearchBar from "../search-bar/SearchBar";

import {
  clearSearchQuery,
  toggleShowMismatches,
  useSearchTrees,
} from "./actions";
import type { SearchT, TreesT } from "./reducer";

const Root = styled("div")`
  position: relative;
`;

const TinyText = styled("p")`
  margin: 3px 0;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
`;

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
`;

const Displaying = styled("span")`
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.gray};
`;

const StyledButton = styled(TransparentButton)`
  margin: 3px 0 3px 5px;
`;

const TopRow = styled("div")`
  display: flex;
  align-items: center;
`;
const SxConceptTreeOpenButtons = styled(ConceptTreesOpenButtons)`
  margin-right: 5px;
`;

interface PropsT {
  className?: string;
}

const ConceptTreeSearchBox: FC<PropsT> = ({ className }) => {
  const showMismatches = useSelector<StateT, boolean>(
    (state) => state.conceptTrees.search.showMismatches,
  );
  const search = useSelector<StateT, SearchT>(
    (state) => state.conceptTrees.search,
  );
  const trees = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  const dispatch = useDispatch();
  const { t } = useTranslation();

  const searchTrees = useSearchTrees();
  const onSearch = (trees: TreesT, searchString: string) => {
    if (searchString.length > 1) {
      searchTrees(trees, searchString);
    }
  };
  const onClearQuery = () => dispatch(clearSearchQuery());
  const onToggleShowMismatches = () => dispatch(toggleShowMismatches());

  return (
    <Root className={className}>
      <TopRow>
        <SxConceptTreeOpenButtons />
        <SearchBar
          searchTerm={search.query}
          placeholder={t("conceptTreeList.searchPlaceholder")}
          onClear={onClearQuery}
          onSearch={(value) => onSearch(trees, value)}
        />
      </TopRow>
      {search.loading ? (
        <AnimatedDots />
      ) : (
        search.result &&
        search.resultCount >= 0 && (
          <Row>
            <TinyText>
              {t("search.resultLabel", {
                totalResults: search.resultCount,
                duration: (search.duration / 1000.0).toFixed(2),
              })}
            </TinyText>
            <div>
              <Displaying>
                {showMismatches
                  ? t("conceptTreeList.showingMismatches")
                  : t("conceptTreeList.showingMatchesOnly")}
              </Displaying>
              <StyledButton tiny onClick={onToggleShowMismatches}>
                {showMismatches
                  ? t("conceptTreeList.showMatchesOnly")
                  : t("conceptTreeList.showMismatches")}
              </StyledButton>
            </div>
          </Row>
        )
      )}
    </Root>
  );
};

export default ConceptTreeSearchBox;
