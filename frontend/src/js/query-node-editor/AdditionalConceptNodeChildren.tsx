import { memo } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { ConceptBaseT, ConceptIdT } from "../api/types";
import { Heading4 } from "../headings/Headings";
import type { DragItemConceptTreeNode } from "../standard-query-editor/types";

import ConceptDropzone from "./ConceptDropzone";
import ConceptEntry from "./ConceptEntry";
import { HeadingBetween } from "./HeadingBetween";

const padded = tv({
  base: ["flex flex-col", "h-full", "overflow-hidden", "px-[15px] pb-[15px]"],
});

const scrollable = tv({
  base: ["h-full", "overflow-y-auto", "[-webkit-overflow-scrolling:touch]"],
});

const heading = tv({
  base: ["text-primary-500", "font-bold", "mt-[10px] mb-[5px]"],
});

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
      <HeadingBetween>{t("queryNodeEditor.dropMoreConcepts")}</HeadingBetween>
      <div className={padded()}>
        <Heading4 className={heading()}>{rootConcept.label}</Heading4>
        <div>
          <ConceptDropzone node={node} onDropConcept={onDropConcept} />
        </div>
        <div className={scrollable()}>
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
