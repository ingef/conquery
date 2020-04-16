import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import type { ConceptT, ConceptIdT } from "../api/types";
import FaIcon from "../icon/FaIcon";
import IconButton from "../button/IconButton";

import ConceptTreeNode from "./ConceptTreeNode";
import ConceptTreeNodeText from "./ConceptTreeNodeText";
import type { SearchT } from "./reducer";

type PropsType = {
  id: ConceptIdT,
  tree: ConceptT | null,
  treeId: ConceptIdT,
  label: string,
  description: string | null,
  depth: number,
  loading: boolean,
  error: string | null,
  search?: SearchT
};

const LoadingTree = styled("p")`
  padding-left: ${({ depth }) => 24 + depth * 15}px;
  font-size: ${({ theme }) => theme.font.sm};
  margin: 2px 0;
  line-height: 20px;
`;
const ErrorMessage = styled("p")`
  color: ${({ theme }) => theme.col.red};
  font-weight: 400;
  padding-left: ${({ depth }) => 12 + depth * 15}px;
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

export default (props: PropsType) => {
  if (props.loading)
    return (
      <LoadingTree depth={props.depth}>
        <Spinner>
          <FaIcon icon="spinner" />
        </Spinner>
        <span>{props.label}</span>
      </LoadingTree>
    );
  else if (props.error)
    return (
      <ErrorMessage depth={props.depth}>
        <ReloadButton
          red
          icon="redo"
          onClick={() => props.onLoadTree(props.treeId)}
        />
        {T.translate("conceptTreeList.error", { tree: props.label })}
      </ErrorMessage>
    );
  else if (props.tree)
    return (
      <ConceptTreeNode
        id={props.id}
        data={{ ...props.tree, tree: props.treeId }}
        depth={props.depth}
        search={props.search}
      />
    );
  else
    return (
      <ConceptTreeNodeText disabled label={props.label} depth={props.depth} />
    );
};
