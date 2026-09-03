import { faTrashCan } from "@fortawesome/free-regular-svg-icons";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { ConceptIdT, ConceptT } from "../api/types";
import { getConceptById } from "../concept-trees/globalTreeStoreHelper";
import AdditionalInfoHoverable from "../info-pane/AdditionalInfoHoverable";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

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

const headline = tv({
  base: ["m-0", "text-sm", "font-normal"],
  variants: {
    notFound: { true: "text-red" },
  },
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
          <h6 className={headline({ notFound: true })}>
            {t("queryNodeEditor.nodeNotFound")}
          </h6>
        ) : (
          <>
            <h6 className={headline()}>{node.label}</h6>
            {node.description && (
              <p className="m-0 text-xs">{node.description}</p>
            )}
          </>
        )}
      </div>
      {canRemoveConcepts && (
        <Button
          intent="tertiary"
          size="sm"
          onPress={() => onRemoveConcept(conceptId)}
          className="shrink-0"
        >
          <Icon icon={faTrashCan} />
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
