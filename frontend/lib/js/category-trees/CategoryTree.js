// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import type { NodeType, TreeNodeIdType } from "../common/types/backend";
import FaIcon from "../icon/FaIcon";
import IconButton from "../button/IconButton";

import CategoryTreeNode from "./CategoryTreeNode";
import { type SearchType } from "./reducer";

type PropsType = {
  id: TreeNodeIdType,
  tree: NodeType,
  treeId: TreeNodeIdType,
  label: string,
  depth: number,
  loading: boolean,
  error: ?string,
  search?: SearchType
};

const LoadingTree = styled("p")`
  padding-left: 26px;
  font-size: ${({ theme }) => theme.font.sm};
  margin: 1px 0;
  line-height: 22px;
`;
const ErrorMessage = styled("p")`
  color: ${({ theme }) => theme.col.red};
  font-weight: 400;
  padding-left: 14px;
  font-size: ${({ theme }) => theme.font.sm};
  margin: 1px 0;
  line-height: 22px;
`;

const Spinner = styled("span")`
  margin-right: 5px;
`;

const CategoryTree = (props: PropsType) => {
  if (props.loading)
    return (
      <LoadingTree>
        <Spinner>
          <FaIcon icon="spinner" />
        </Spinner>
        <span>
          {T.translate("categoryTreeList.loadingTree", { tree: props.label })}
        </span>
      </LoadingTree>
    );
  else if (props.error)
    return (
      <ErrorMessage>
        <IconButton
          red
          icon="redo"
          onClick={() => props.onLoadTree(props.treeId)}
        />
        {T.translate("categoryTreeList.error", { tree: props.label })}
      </ErrorMessage>
    );
  else if (props.tree)
    return (
      <CategoryTreeNode
        id={props.id}
        data={{ ...props.tree, tree: props.treeId }}
        depth={props.depth}
        search={props.search}
      />
    );
  else return null;
};

export default CategoryTree;
