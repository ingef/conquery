import { useMemo } from "react";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { exists } from "../common/helpers/exists";

import type { TreesT } from "./reducer";

const isRootTreeId = (trees: TreesT) => (treeId: string) => {
  const tree = trees[treeId];
  const hasParent =
    tree.hasOwnProperty("parent") && exists((tree as any).parent);
  const isntLoadingOrBrokenSomehow = exists(tree.label);

  return !hasParent && isntLoadingOrBrokenSomehow;
};

export const useRootConceptIds = () => {
  const trees = useSelector<StateT, TreesT>(
    (state) => state.conceptTrees.trees,
  );

  return useMemo(() => Object.keys(trees).filter(isRootTreeId(trees)), [trees]);
};
