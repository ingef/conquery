import { useMemo } from "react";
import { useSelector } from "react-redux";

import type { DatasetT } from "../api/types";
import type { StateT } from "../app/reducers";

import tw from "tailwind-styled-components";
import ConceptTreeListItem from "./ConceptTreeListItem";
import ConceptTreesLoading from "./ConceptTreesLoading";
import ConceptsProgressBar from "./ConceptsProgressBar";
import EmptyConceptTreeList from "./EmptyConceptTreeList";
import { useLoadTree } from "./actions";
import type { SearchT, TreesT } from "./reducer";
import { useAreTreesAvailable } from "./selectors";
import { useRootConceptIds } from "./useRootConceptIds";

/**
  @param show For historic reasons, it was necessary to only hide / show the concept tree list,
  instead of mounting / unmounting it. Maybe we can remove this in the future.
*/
const Root = tw("div")<{ $show?: boolean }>`
  flex-grow
  shrink-0
  basis-0
  px-[10px]
  overflow-y-auto
  whitespace-nowrap
  mb-[10px]

  ${({ $show }) => ($show ? "block" : "hidden")}
`;

const ConceptTreeList = ({
  datasetId,
}: {
  datasetId: DatasetT["id"] | null;
}) => {
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
    <Root $show={activeTab === "conceptTrees"}>
      {loading && <ConceptTreesLoading />}
      {!loading && !areTreesAvailable && !areDatasetsPristineOrLoading && (
        <EmptyConceptTreeList />
      )}
      {!!anyTreeLoading && <ConceptsProgressBar trees={trees} />}
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
