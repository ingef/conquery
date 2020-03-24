import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";

import TransparentButton from "../button/TransparentButton";
import IconButton from "../button/IconButton";

import { isEmpty } from "../common/helpers";
import AnimatedDots from "../common/components/AnimatedDots";

import BaseInput from "../form-components/BaseInput";

import type { DatasetIdT } from "../api/types";

import { getAreTreesAvailable } from "./selectors";
import { searchTrees, clearSearchQuery, toggleShowMismatches } from "./actions";

import type { SearchT, TreesT } from "./reducer";

const Root = styled("div")`
  margin: 0 10px 5px;
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

type PropsT = {
  datasetId: string,

  trees: TreesT,
  search: SearchT,
  areTreesAvailable: boolean,
  showMismatches: boolean,

  onSearch: (datasetId: DatasetIdT, trees: TreesT, value: string) => void,
  onChange: (val: string) => void,
  onClearQuery: () => void,
  onToggleShowMismatches: () => void
};

const mapStateToProps = state => ({
  areTreesAvailable: getAreTreesAvailable(state),
  showMismatches: state.conceptTrees.search.showMismatches,
  search: state.conceptTrees.search,
  trees: state.conceptTrees.trees
});

const mapDispatchToProps = dispatch => ({
  onSearch: (datasetId, trees, query) => {
    if (query.length > 1) dispatch(searchTrees(datasetId, trees, query));
  },
  onClearQuery: () => dispatch(clearSearchQuery()),
  onToggleShowMismatches: () => dispatch(toggleShowMismatches())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(
  ({
    datasetId,
    search,
    trees,
    onSearch,
    onClearQuery,
    showMismatches,
    onToggleShowMismatches,
    areTreesAvailable
  }: PropsT) => {
    const [localQuery, setLocalQuery] = React.useState("");

    React.useEffect(() => {
      setLocalQuery(search.query);
    }, [search.query]);

    if (!areTreesAvailable) return null;

    return (
      <Root>
        <StyledBaseInput
          placeholder={T.translate("conceptTreeList.searchPlaceholder")}
          value={localQuery || ""}
          onChange={value => {
            if (isEmpty(value)) onClearQuery();

            setLocalQuery(value);
          }}
          inputProps={{
            onKeyPress: e => {
              return e.key === "Enter" && !isEmpty(e.target.value)
                ? onSearch(datasetId, trees, e.target.value)
                : null;
            }
          }}
        />
        {!isEmpty(localQuery) && (
          <Right>
            <StyledIconButton
              icon="search"
              aria-hidden="true"
              onClick={() => onSearch(datasetId, trees, localQuery)}
            />
          </Right>
        )}
        {search.loading ? (
          <AnimatedDots />
        ) : (
          search.result &&
          search.resultCount >= 0 && (
            <Row>
              <TinyText>
                {T.translate("search.resultLabel", {
                  totalResults: search.resultCount,
                  duration: (search.duration / 1000.0).toFixed(2)
                })}
              </TinyText>
              <div>
                <Displaying>
                  {showMismatches
                    ? T.translate("conceptTreeList.showingMismatches")
                    : T.translate("conceptTreeList.showingMatchesOnly")}
                </Displaying>
                <StyledButton tiny onClick={onToggleShowMismatches}>
                  {showMismatches
                    ? T.translate("conceptTreeList.showMatchesOnly")
                    : T.translate("conceptTreeList.showMismatches")}
                </StyledButton>
              </div>
            </Row>
          )
        )}
      </Root>
    );
  }
);
