// @flow

import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";

import { loadTree } from "./actions";

import type { StateType } from "../app/reducers";

import { getConceptById } from "./globalTreeStoreHelper";
import { type TreesT, type SearchType } from "./reducer";
import { isNodeInSearchResult, getAreTreesAvailable } from "./selectors";

import EmptyConceptTreeList from "./EmptyConceptTreeList";
import ConceptTreesLoading from "./ConceptTreesLoading";
import ConceptTree from "./ConceptTree";
import ConceptTreeFolder from "./ConceptTreeFolder";

const Root = styled("div")`
  flex-grow: 1;
  flex-shrink: 0;
  flex-basis: 0;
  overflow-y: auto;
  padding: 0 10px 0;
  white-space: nowrap;

  ${
    ""
    // Only hide the Concept trees when the tab is not selected
    // Because mount / unmount would reset the open states
    // that are React states and not part of the Redux state
    // because if they were part of Redux state, the entire tree
    // would have to re-render when a single node would be opened
    //
    // Also: Can't set it to initial, because IE11 doesn't work then
    // => Empty string instead
  }
  display: ${({ show }) => (show ? "" : "none")};
`;

type PropsT = {
  loading: boolean,
  trees: TreesT,
  areTreesAvailable: boolean,
  activeTab: string,
  search?: SearchType,
  onLoadTree: (id: string) => void
};

const ConceptTreeList = ({
  loading,
  trees,
  search,
  activeTab,
  areTreesAvailable,
  onLoadTree
}: PropsT) => {
  return (
    !search.loading && (
      <Root show={activeTab === "conceptTrees"}>
        {loading && <ConceptTreesLoading />}
        {!loading && !areTreesAvailable && <EmptyConceptTreeList />}
        {Object.keys(trees)
          // Only take those that don't have a parent, they must be root
          // If they don't have a label, they're loading, or in any other broken state
          .filter(treeId => !trees[treeId].parent && trees[treeId].label)
          .map((treeId, i) => {
            const tree = trees[treeId];
            const rootConcept = getConceptById(treeId);

            const render = isNodeInSearchResult(treeId, tree.children, search);

            if (!render) return null;

            const commonProps = {
              treeId,
              search,
              onLoadTree,
              key: i,
              depth: 0
            };

            return tree.detailsAvailable ? (
              <ConceptTree
                id={treeId}
                label={tree.label}
                description={tree.description}
                tree={rootConcept}
                loading={!!tree.loading}
                error={tree.error}
                {...commonProps}
              />
            ) : (
              <ConceptTreeFolder
                trees={trees}
                tree={tree}
                active={tree.active}
                openInitially
                {...commonProps}
              />
            );
          })}
      </Root>
    )
  );
};

export default connect(
  (state: StateType) => ({
    trees: state.conceptTrees.trees,
    loading: state.conceptTrees.loading,
    areTreesAvailable: getAreTreesAvailable(state),
    activeTab: state.panes.left.activeTab,
    search: state.conceptTrees.search
  }),
  (dispatch, ownProps) => ({
    onLoadTree: id => dispatch(loadTree(ownProps.datasetId, id))
  })
)(ConceptTreeList);
