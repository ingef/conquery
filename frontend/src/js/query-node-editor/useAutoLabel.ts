import { useEffect, useMemo, useRef, useState } from "react";

import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { nodeIsConceptQueryNode } from "../model/node";
import { StandardQueryNodeT } from "../standard-query-editor/types";

interface AutoLabelProps {
  node: StandardQueryNodeT;
  onUpdateLabel: (label: string) => void;
  onUpdateDescription: (label: string) => void;
}

export function useAutoLabel({
  node,
  onUpdateLabel,
  onUpdateDescription,
}: AutoLabelProps) {
  const MAX_AUTOLABEL_LENGTH = 250;
  const DELIMITER = " ";
  const previousNodeIdsLength = useRef(
    nodeIsConceptQueryNode(node) ? node.ids.length : -1,
  );

  const formatConceptLabels = (labels: string[]) =>
    labels.sort().join(DELIMITER).substring(0, MAX_AUTOLABEL_LENGTH);

  const autoLabel = useMemo(() => {
    return nodeIsConceptQueryNode(node)
      ? formatConceptLabels(
        node.ids.map((id) => getConceptById(id)?.label ?? ""),
      )
      : undefined;
  }, [node]);

  const [autoLabelEnabled, setAutoLabelEnabled] = useState(
    nodeIsConceptQueryNode(node) && node.label === autoLabel,
  );

  const onUpdateLabelRef = useRef(onUpdateLabel);
  onUpdateLabelRef.current = onUpdateLabel;
  const onUpdateDescriptionRef = useRef(onUpdateDescription);
  onUpdateDescriptionRef.current = onUpdateDescription;
  useEffect(
    function updateLabelAndDescription() {
      if (!nodeIsConceptQueryNode(node)) return;

      if (autoLabelEnabled && autoLabel && node.label !== autoLabel) {
        onUpdateLabelRef.current(autoLabel);
      }

      if (node.ids.length !== previousNodeIdsLength.current) {
        previousNodeIdsLength.current = node.ids.length;
        if (node.ids.length === 1) {
          const description = getConceptById(node.ids[0])?.description;
          if (description) {
            onUpdateDescriptionRef.current(description);
          }
        }
      }
    },
    [autoLabel, autoLabelEnabled, node],
  );

  return { autoLabel, autoLabelEnabled, setAutoLabelEnabled };
}
