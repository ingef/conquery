// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { connect } from "react-redux";

import { SearchBox } from "../form-components";

import { getAreTreesAvailable } from "./selectors";
import {
  searchTrees,
  changeSearchQuery,
  clearSearchQuery,
  toggleShowMismatches
} from "./actions";

import TransparentButton from "../button/TransparentButton";

const Displaying = styled("span")`
  font-size: ${({ theme }) => theme.font.xs};
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.gray};
`;

const StyledButton = styled(TransparentButton)`
  margin: 3px 0 3px 5px;
`;

const CategoryTreeSearchBox = ({
  showMismatches,
  onToggleShowMismatches,
  areTreesAvailable,
  ...props
}) => {
  if (!areTreesAvailable) return null;

  return (
    <SearchBox
      {...props}
      placeholder={T.translate("categoryTreeList.searchPlaceholder")}
      textAppend={
        <div>
          <Displaying>
            {showMismatches
              ? T.translate("categoryTreeList.showingMismatches")
              : T.translate("categoryTreeList.showingMatchesOnly")}
          </Displaying>
          <StyledButton tiny onClick={onToggleShowMismatches}>
            {showMismatches
              ? T.translate("categoryTreeList.showMatchesOnly")
              : T.translate("categoryTreeList.showMismatches")}
          </StyledButton>
        </div>
      }
    />
  );
};

const mapStateToProps = state => ({
  areTreesAvailable: getAreTreesAvailable(state),
  showMismatches: state.categoryTrees.search.showMismatches,
  search: state.categoryTrees.search
});

const mapDispatchToProps = dispatch => ({
  onSearch: (datasetId, query) => {
    if (query.length > 1) dispatch(searchTrees(datasetId, query));
  },
  onChange: query => dispatch(changeSearchQuery(query)),
  onClearQuery: () => dispatch(clearSearchQuery()),
  onToggleShowMismatches: () => dispatch(toggleShowMismatches())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CategoryTreeSearchBox);
