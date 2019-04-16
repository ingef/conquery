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
  toggleAllOpen,
  toggleShowMismatches
} from "./actions";

import TransparentButton from "../button/TransparentButton";

const OPENABLE_AT = 500;

const StyledButton = styled(TransparentButton)`
  margin: 3px 0 3px 5px;
`;

const CategoryTreeSearchBox = ({
  allOpen,
  showMismatches,
  onToggleShowMismatches,
  onToggleAllOpen,
  areTreesAvailable,
  ...props
}) => {
  if (!areTreesAvailable) return null;

  return (
    <SearchBox
      {...props}
      placeholder={T.translate("categoryTreeList.searchPlaceholder")}
      textAppend={
        <>
          <StyledButton tiny onClick={onToggleShowMismatches}>
            {showMismatches
              ? T.translate("categoryTreeList.dontShowMismatches")
              : T.translate("categoryTreeList.showMismatches")}
          </StyledButton>
          {!showMismatches && props.search.resultCount < OPENABLE_AT && (
            <StyledButton tiny onClick={onToggleAllOpen}>
              {allOpen
                ? T.translate("categoryTreeList.closeAll")
                : T.translate("categoryTreeList.openAll")}
            </StyledButton>
          )}
        </>
      }
    />
  );
};

const mapStateToProps = state => ({
  areTreesAvailable: getAreTreesAvailable(state),
  allOpen: state.categoryTrees.search.allOpen,
  showMismatches: state.categoryTrees.search.showMismatches,
  search: state.categoryTrees.search
});

const mapDispatchToProps = dispatch => ({
  onSearch: (datasetId, query) => {
    if (query.length > 1) dispatch(searchTrees(datasetId, query));
  },
  onChange: query => dispatch(changeSearchQuery(query)),
  onClearQuery: () => dispatch(clearSearchQuery()),
  onToggleAllOpen: () => dispatch(toggleAllOpen()),
  onToggleShowMismatches: () => dispatch(toggleShowMismatches())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CategoryTreeSearchBox);
