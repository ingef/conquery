import { useEffect, useMemo, useState } from "react";

import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import { nodeIsConceptQueryNode } from "../model/node";
import { StandardQueryNodeT } from "../standard-query-editor/types";

interface AutoLabelProps {
  node: StandardQueryNodeT;
  onUpdateLabel: (label: string) => void;
}

export function useAutoLabel({ node, onUpdateLabel }: AutoLabelProps) {
  const MAX_AUTOLABEL_LENGTH = 30;

  const formatConceptLabels = (labels: string[]) =>
    labels
      .map((label) => label.match(/[a-zA-Z0-9]*/g)?.join(""))
      .join("_")
      .substring(0, MAX_AUTOLABEL_LENGTH);

  const autoLabel = useMemo(() => {
    return nodeIsConceptQueryNode(node)
      ? formatConceptLabels(
          node.ids.map((id) => getConceptById(id)?.label ?? ""),
        )
      : undefined;
  }, [node]);

  const [autoLabelEnabled, setAutoLabelEnabled] = useState(
    nodeIsConceptQueryNode(node) &&
      formatConceptLabels(node.label.split("_")) === autoLabel,
  );

  // save auto generated node label
  useEffect(() => {
    if (autoLabelEnabled && autoLabel && node.label !== autoLabel) {
      onUpdateLabel(autoLabel);
    }
  }, [autoLabel, autoLabelEnabled, node.label, onUpdateLabel]);

  return { autoLabel, autoLabelEnabled, setAutoLabelEnabled };
}
