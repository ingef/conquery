import React from "react";
import styled from "@emotion/styled";
import { connect } from "react-redux";

import { loadTree } from "./actions";

import type { StateT } from "../app/reducers";

import type { TreesT, SearchT } from "./reducer";
import { getAreTreesAvailable } from "./selectors";

import EmptyConceptTreeList from "./EmptyConceptTreeList";
import ConceptTreesLoading from "./ConceptTreesLoading";
import ProgressBar from "./ProgressBar";
import ConceptTreeListItem from "./ConceptTreeListItem";

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
  loading: boolean;
  trees: TreesT;
  areTreesAvailable: boolean;
  areDatasetsPristineOrLoading: boolean;
  activeTab: string;
  search: SearchT;
  onLoadTree: (id: string) => void;
};

const ConceptTreeList = ({
  loading,
  trees,
  search,
  activeTab,
  areTreesAvailable,
  areDatasetsPristineOrLoading,
  onLoadTree
}: PropsT) => {
  if (search.loading) return null;

  const anyTreeLoading = Object.keys(trees).some(
    treeId => trees[treeId].loading
  );

  return (
    <Root show={activeTab === "conceptTrees"}>
      {loading && <ConceptTreesLoading />}
      {!loading && !areTreesAvailable && !areDatasetsPristineOrLoading && (
        <EmptyConceptTreeList />
      )}
      {!!anyTreeLoading && <ProgressBar trees={trees} />}
      {!anyTreeLoading &&
        Object.keys(trees)
          // Only take those that don't have a parent, they must be root
          // If they don't have a label, they're loading, or in any other broken state
          .filter(treeId => !trees[treeId].parent && trees[treeId].label)
          .map((treeId, i) => (
            <ConceptTreeListItem
              key={i}
              search={search}
              onLoadTree={onLoadTree}
              trees={trees}
              treeId={treeId}
            />
          ))}
    </Root>
  );
};

export default connect(
  (state: StateT) => ({
    trees: state.conceptTrees.trees,
    loading: state.conceptTrees.loading,
    areTreesAvailable: getAreTreesAvailable(state),
    areDatasetsPristineOrLoading:
      state.datasets.pristine || state.datasets.loading,
    activeTab: state.panes.left.activeTab,
    search: state.conceptTrees.search
  }),
  (dispatch, ownProps) => ({
    onLoadTree: id => dispatch(loadTree(ownProps.datasetId, id))
  })
)(ConceptTreeList);
