import React, { FC } from "react";
import styled from "@emotion/styled";
import { useSelector, useDispatch } from "react-redux";

import { loadTree } from "./actions";

import type { StateT } from "../app/reducers";

import type { TreesT, SearchT } from "./reducer";
import { getAreTreesAvailable } from "./selectors";

import EmptyConceptTreeList from "./EmptyConceptTreeList";
import ConceptTreesLoading from "./ConceptTreesLoading";
import ProgressBar from "./ProgressBar";
import ConceptTreeListItem from "./ConceptTreeListItem";
import type { DatasetIdT } from "../api/types";
import { useRootConceptIds } from "./useRootConceptIds";

const Root = styled("div")<{ show?: boolean }>`
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

interface PropsT {
  datasetId: DatasetIdT;
}

const ConceptTreeList: FC<PropsT> = ({ datasetId }) => {
  const trees = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees
  );
  const loading = useSelector<StateT, boolean>(
    (state) => state.conceptTrees.loading
  );
  const areTreesAvailable = useSelector<StateT, boolean>((state) =>
    getAreTreesAvailable(state)
  );
  const areDatasetsPristineOrLoading = useSelector<StateT, boolean>(
    (state) => state.datasets.pristine || state.datasets.loading
  );
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.left.activeTab
  );
  const search = useSelector<StateT, SearchT>(
    (state) => state.conceptTrees.search
  );

  const dispatch = useDispatch();

  const onLoadTree = (id: string) => dispatch(loadTree(datasetId, id));

  const rootConceptIds = useRootConceptIds();

  if (search.loading) return null;

  const anyTreeLoading = Object.keys(trees).some(
    (treeId) => trees[treeId].loading
  );

  return (
    <Root show={activeTab === "conceptTrees"}>
      {loading && <ConceptTreesLoading />}
      {!loading && !areTreesAvailable && !areDatasetsPristineOrLoading && (
        <EmptyConceptTreeList />
      )}
      {!!anyTreeLoading && <ProgressBar trees={trees} />}
      {!anyTreeLoading &&
        rootConceptIds.map((treeId, i) => (
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

export default ConceptTreeList;
