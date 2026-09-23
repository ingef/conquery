import { TrashIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { ConceptIdT, ConceptT } from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import AdditionalInfoHoverable from "../info-pane/AdditionalInfoHoverable";
import { Button } from "../ui-components/Button";
import { C2, C3 } from "../ui-components/Typography";

const concept = tv({
  base: [
    "flex flex-row items-center",
    "mt-[5px]",
    "rounded",
    "border border-gray-500",
    "bg-white",
    "px-[15px] py-[5px]",
  ],
});

interface Props {
  conceptId: ConceptIdT;
  root: ConceptT;
  canRemoveConcepts?: boolean;
  onRemoveConcept: (conceptId: ConceptIdT) => void;
}

const ConceptEntry = ({
  conceptId,
  root,
  canRemoveConcepts,
  onRemoveConcept,
}: Props) => {
  const { t } = useTranslation();
  const node = getConceptById(conceptId);

  const ConceptEntryRoot = (
    <div className={concept()}>
      <div className="grow">
        {!node ? (
          <C2 tone="danger">{t("queryNodeEditor.nodeNotFound")}</C2>
        ) : (
          <>
            <C2>{node.label}</C2>
            {node.description && <C3 tone="muted">{node.description}</C3>}
          </>
        )}
      </div>
      {canRemoveConcepts && (
        <Button
          intent="tertiary"
          size="sm"
          aria-label={t("common.delete")}
          onPress={() => onRemoveConcept(conceptId)}
        >
          <TrashIcon />
        </Button>
      )}
    </div>
  );

  return node && root ? (
    <AdditionalInfoHoverable node={node} root={root}>
      {ConceptEntryRoot}
    </AdditionalInfoHoverable>
  ) : (
    ConceptEntryRoot
  );
};

export default ConceptEntry;
