import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import type { DatasetIdT } from "../api/types";
import IconButton from "../button/IconButton";
import TransparentButton from "../button/TransparentButton";
import AnimatedDots from "../common/components/AnimatedDots";
import { isEmpty } from "../common/helpers";
import ConceptTreesOpenButtons from "../concept-trees-open/ConceptTreesOpenButtons";
import BaseInput from "../form-components/BaseInput";

import { searchTrees, clearSearchQuery, toggleShowMismatches } from "./actions";
import type { SearchT, TreesT } from "./reducer";

const Root = styled("div")`
  position: relative;
`;

const InputContainer = styled("div")`
  flex-grow: 1;
  position: relative;
`;

const StyledBaseInput = styled(BaseInput)`
  width: 100%;
  input {
    width: 100%;
    &::placeholder {
      color: ${({ theme }) => theme.col.grayMediumLight};
      opacity: 1;
    }
  }
`;

const Right = styled("div")`
  position: absolute;
  top: 0px;
  right: 30px;
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 36px;
`;

const StyledIconButton = styled(IconButton)`
  color: ${({ theme }) => theme.col.gray};
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
  datasetId: DatasetIdT;
  className?: string;
}

const ConceptTreeSearchBox: FC<PropsT> = ({ className, datasetId }) => {
  const [localQuery, setLocalQuery] = useState("");

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

  const onSearch = (datasetId: DatasetIdT, trees: TreesT, query: string) => {
    if (query.length > 1) dispatch(searchTrees(datasetId, trees, query));
  };
  const onClearQuery = () => dispatch(clearSearchQuery());
  const onToggleShowMismatches = () => dispatch(toggleShowMismatches());

  useEffect(() => {
    setLocalQuery(search.query);
  }, [search.query]);

  return (
    <Root className={className}>
      <TopRow>
        <SxConceptTreeOpenButtons />
        <InputContainer>
          <StyledBaseInput
            placeholder={t("conceptTreeList.searchPlaceholder")}
            value={localQuery || ""}
            onChange={(value) => {
              if (isEmpty(value)) onClearQuery();

              setLocalQuery(value);
            }}
            inputProps={{
              onKeyPress: (e) => {
                return e.key === "Enter" && !isEmpty(e.target.value)
                  ? onSearch(datasetId, trees, e.target.value)
                  : null;
              },
            }}
          />
          {!isEmpty(localQuery) && (
            <Right>
              <StyledIconButton
                icon="search"
                aria-hidden="true"
                small
                onClick={() => onSearch(datasetId, trees, localQuery)}
              />
            </Right>
          )}
        </InputContainer>
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
