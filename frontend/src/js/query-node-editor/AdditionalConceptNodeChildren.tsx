import { memo } from "react";
import { useTranslation } from "react-i18next";

import type { ConceptBaseT, ConceptIdT } from "../api/types";
import type { DragItemConceptTreeNode } from "../standard-query-editor/types";
import { C2, H4 } from "../ui-components/Typography";
import ConceptDropzone from "./ConceptDropzone";
import ConceptEntry from "./ConceptEntry";

const AdditionalConceptNodeChildren = ({
  node,
  rootConcept,
  onRemoveConcept,
  onDropConcept,
}: {
  node: DragItemConceptTreeNode;
  rootConcept: ConceptBaseT;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
  onDropConcept: (node: DragItemConceptTreeNode) => void;
}) => {
  const { t } = useTranslation();

  const sortedNodeIds = [...node.ids].sort();

  return (
    <>
      <H4>{t("queryNodeEditor.dropMoreConcepts")}</H4>
      <div className="flex min-h-0 grow flex-col gap-2">
        <C2 tone="muted">{rootConcept.label}</C2>
        <ConceptDropzone node={node} onDropConcept={onDropConcept} />
        <div className="flex min-h-0 flex-col gap-1 overflow-y-auto [-webkit-overflow-scrolling:touch]">
          {sortedNodeIds.map((conceptId) => (
            <ConceptEntry
              key={conceptId}
              conceptId={conceptId}
              root={rootConcept}
              canRemoveConcepts={node.ids.length > 1}
              onRemoveConcept={onRemoveConcept}
            />
          ))}
        </div>
      </div>
    </>
  );
};

export default memo(AdditionalConceptNodeChildren);
