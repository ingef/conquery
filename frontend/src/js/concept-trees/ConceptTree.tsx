import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { ConceptT, ConceptIdT } from "../api/types";
import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";

import ConceptTreeNode from "./ConceptTreeNode";
import ConceptTreeNodeText from "./ConceptTreeNodeText";
import type { SearchT } from "./reducer";

interface PropsT {
  id: ConceptIdT;
  tree: ConceptT | null;
  treeId: ConceptIdT;
  label: string;
  description: string | null;
  depth: number;
  loading: boolean;
  error: string | null;
  search?: SearchT;
  onLoadTree: (treeId: ConceptIdT) => void;
}

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

const ConceptTree: FC<PropsT> = ({
  id,
  depth,
  loading,
  label,
  error,
  tree,
  treeId,
  search,
  onLoadTree,
}) => {
  const { t } = useTranslation();

  if (loading)
    return (
      <LoadingTree style={{ paddingLeft: 24 + depth * 15 }}>
        <Spinner>
          <FaIcon icon="spinner" />
        </Spinner>
        <span>{label}</span>
      </LoadingTree>
    );
  else if (error)
    return (
      <ErrorMessage style={{ paddingLeft: 12 + depth * 15 }}>
        <ReloadButton red icon="redo" onClick={() => onLoadTree(treeId)} />
        {t("conceptTreeList.error", { tree: label })}
      </ErrorMessage>
    );
  else if (tree)
    return (
      <ConceptTreeNode
        id={id}
        data={{ ...tree, tree: treeId }}
        depth={depth}
        search={search}
      />
    );
  else return <ConceptTreeNodeText disabled label={label} depth={depth} />;
};

export default ConceptTree;
