import { LoaderCircleIcon, RotateCwIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { ConceptIdT, ConceptT } from "../api/types";
import { Button } from "../ui-components/Button";
import { C2 } from "../ui-components/Typography";
import ConceptTreeNode from "./ConceptTreeNode";
import ConceptTreeNodeText from "./ConceptTreeNodeText";
import type { SearchT } from "./reducer";

const message = tv({
  base: ["flex items-center", "my-[2px]"],
  variants: {
    error: {
      true: "text-red",
    },
  },
});

const ConceptTree = ({
  depth,
  loading,
  label,
  error,
  tree,
  conceptId,
  search,
  onLoadTree,
}: {
  tree?: ConceptT;
  conceptId: ConceptIdT;
  label: string;
  depth: number;
  loading?: boolean;
  error?: string;
  search: SearchT;
  onLoadTree: (conceptId: ConceptIdT) => void;
}) => {
  const { t } = useTranslation();

  if (loading)
    return (
      <div className={message()} style={{ paddingLeft: 24 + depth * 15 }}>
        <span className="mr-[6px]">
          <LoaderCircleIcon />
        </span>
        <C2 as="span">{label}</C2>
      </div>
    );
  else if (error)
    return (
      <div
        className={message({ error: true })}
        style={{ paddingLeft: 12 + depth * 15 }}
      >
        <Button
          intent="tertiary"
          size="sm"
          danger
          aria-label={t("common.retry")}
          onPress={() => onLoadTree(conceptId)}
        >
          <RotateCwIcon />
        </Button>
        <C2 as="span">{t("conceptTreeList.error", { tree: label })}</C2>
      </div>
    );
  else if (tree)
    return (
      <ConceptTreeNode
        conceptId={conceptId}
        rootConceptId={conceptId}
        data={tree}
        depth={depth}
        search={search}
      />
    );
  else
    return (
      <ConceptTreeNodeText
        disabled
        icon="pending"
        label={label}
        depth={depth}
      />
    );
};

export default ConceptTree;
