import {
  faEllipsisH,
  faRedo,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { ConceptIdT, ConceptT } from "../api/types";
import IconButton from "../button/IconButton";
import { Icon } from "../ui-components/Icon";
import ConceptTreeNode from "./ConceptTreeNode";
import ConceptTreeNodeText from "./ConceptTreeNodeText";
import type { SearchT } from "./reducer";

const message = tv({
  base: ["my-[2px]", "text-sm", "leading-5"],
  variants: {
    error: {
      true: ["text-red", "font-normal"],
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
      <p className={message()} style={{ paddingLeft: 24 + depth * 15 }}>
        <span className="mr-[6px]">
          <Icon icon={faSpinner} />
        </span>
        <span>{label}</span>
      </p>
    );
  else if (error)
    return (
      <p
        className={message({ error: true })}
        style={{ paddingLeft: 12 + depth * 15 }}
      >
        <IconButton
          className="py-0 pr-[7px] pl-3"
          red
          icon={faRedo}
          onClick={() => onLoadTree(conceptId)}
        />
        {t("conceptTreeList.error", { tree: label })}
      </p>
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
        icon={faEllipsisH}
        label={label}
        depth={depth}
      />
    );
};

export default ConceptTree;
