import { useCallback, useEffect, useState } from "react";
import { tv } from "tailwind-variants";

import { useDatasetId } from "../dataset/selectors";

import Query from "./Query";
import StandardQueryNodeEditor from "./StandardQueryNodeEditor";

const root = tv({
  base: ["grow", "h-full", "pt-2 px-[10px] pb-[10px]", "overflow-hidden"],
});

export const QueryEditor = () => {
  const [editedNode, setEditedNode] = useState<{
    andIdx: number;
    orIdx: number;
  } | null>(null);

  const datasetId = useDatasetId();
  const onClose = useCallback(() => setEditedNode(null), []);

  // biome-ignore lint/correctness/useExhaustiveDependencies: close the editor whenever the dataset changes
  useEffect(() => {
    onClose();
  }, [datasetId, onClose]);

  return (
    <div className={root()}>
      <Query setEditedNode={setEditedNode} />
      {editedNode && (
        <StandardQueryNodeEditor editedNode={editedNode} onClose={onClose} />
      )}
    </div>
  );
};
