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
  toggleAllOpen
} from "./actions";

import TransparentButton from "../button/TransparentButton";

const StyledButton = styled(TransparentButton)`
  margin: 3px 0 3px 5px;
`;

const CategoryTreeSearchBox = ({
  allOpen,
  onToggleAllOpen,
  areTreesAvailable,
  ...props
}) => {
  if (!areTreesAvailable) return null;

  return (
    <SearchBox
      {...props}
      textAppend={
        <StyledButton tiny onClick={onToggleAllOpen}>
          {allOpen
            ? T.translate("categoryTreeList.closeAll")
            : T.translate("categoryTreeList.openAll")}
        </StyledButton>
      }
    />
  );
};

const mapStateToProps = state => ({
  areTreesAvailable: getAreTreesAvailable(state),
  allOpen: state.categoryTrees.search.allOpen,
  searchResult: state.categoryTrees.search
});

const mapDispatchToProps = dispatch => ({
  onSearch: (datasetId, query) => dispatch(searchTrees(datasetId, query)),
  onChange: query => dispatch(changeSearchQuery(query)),
  onClearQuery: () => dispatch(clearSearchQuery()),
  onToggleAllOpen: () => dispatch(toggleAllOpen())
});

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(CategoryTreeSearchBox);
