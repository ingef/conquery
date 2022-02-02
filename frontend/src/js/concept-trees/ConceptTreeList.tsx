import styled from "@emotion/styled";
import { FC, useMemo } from "react";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";
import type { StateT } from "../app/reducers";

import ConceptTreeListItem from "./ConceptTreeListItem";
import ConceptTreesLoading from "./ConceptTreesLoading";
import EmptyConceptTreeList from "./EmptyConceptTreeList";
import ProgressBar from "./ProgressBar";
import { useLoadTree } from "./actions";
import type { TreesT, SearchT } from "./reducer";
import { useAreTreesAvailable } from "./selectors";
import { useRootConceptIds } from "./useRootConceptIds";

const Root = styled("div")<{ show?: boolean }>`
  flex-grow: 1;
  flex-shrink: 0;
  flex-basis: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 0 10px 0;
  white-space: nowrap;
  margin-bottom: 10px;

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
  datasetId: DatasetIdT | null;
}

const ConceptTreeList: FC<PropsT> = ({ datasetId }) => {
  const trees = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );
  const loading = useSelector<StateT, boolean>(
    (state) => state.conceptTrees.loading,
  );
  const areTreesAvailable = useAreTreesAvailable();
  const areDatasetsPristineOrLoading = useSelector<StateT, boolean>(
    (state) => state.datasets.pristine || state.datasets.loading,
  );
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.left.activeTab,
  );
  const search = useSelector<StateT, SearchT>(
    (state) => state.conceptTrees.search,
  );

  const loadTree = useLoadTree();
  const onLoadTree = (id: string) => {
    if (datasetId) {
      loadTree(datasetId, id);
    }
  };

  const rootConceptIds = useRootConceptIds();

  const anyTreeLoading = useMemo(
    () => Object.keys(trees).some((treeId) => trees[treeId].loading),
    [trees],
  );

  if (search.loading) return null;

  return (
    <Root show={activeTab === "conceptTrees"}>
      {loading && <ConceptTreesLoading />}
      {!loading && !areTreesAvailable && !areDatasetsPristineOrLoading && (
        <EmptyConceptTreeList />
      )}
      {!!anyTreeLoading && <ProgressBar trees={trees} />}
      {!anyTreeLoading &&
        rootConceptIds.map((conceptId, i) => (
          <ConceptTreeListItem
            key={i}
            search={search}
            onLoadTree={onLoadTree}
            trees={trees}
            conceptId={conceptId}
          />
        ))}
    </Root>
  );
};

export default ConceptTreeList;
