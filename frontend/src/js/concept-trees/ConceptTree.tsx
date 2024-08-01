import styled from "@emotion/styled";
import {
  faEllipsisH,
  faRedo,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import type { ConceptIdT, ConceptT } from "../api/types";
import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";

import ConceptTreeNode from "./ConceptTreeNode";
import ConceptTreeNodeText from "./ConceptTreeNodeText";
import type { SearchT } from "./reducer";

const LoadingTree = styled("p")`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 2px 0;
  line-height: 20px;
`;
const ErrorMessage = styled("p")`
  color: ${({ theme }) => theme.col.red};
  font-weight: 400;
  font-size: ${({ theme }) => theme.font.sm};
  margin: 2px 0;
  line-height: 20px;
`;

const ReloadButton = styled(IconButton)`
  padding: 0 7px 0 12px;
`;

const Spinner = styled("span")`
  margin-right: 6px;
`;

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
      <LoadingTree style={{ paddingLeft: 24 + depth * 15 }}>
        <Spinner>
          <FaIcon icon={faSpinner} />
        </Spinner>
        <span>{label}</span>
      </LoadingTree>
    );
  else if (error)
    return (
      <ErrorMessage style={{ paddingLeft: 12 + depth * 15 }}>
        <ReloadButton red icon={faRedo} onClick={() => onLoadTree(conceptId)} />
        {t("conceptTreeList.error", { tree: label })}
      </ErrorMessage>
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
