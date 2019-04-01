// @flow

import React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";
import T from "i18n-react";

import { ErrorMessage } from "../error-message";
import type { NodeType, TreeNodeIdType } from "../common/types/backend";
import FaIcon from "../icon/FaIcon";

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

const otherTextStyles = css`
  padding-left: 20px;
  font-size: $font-sm;
  margin: 2px 0;
`;

const LoadingTree = styled("p")`
  ${otherTextStyles};
`;
const StyledErrorMessage = styled(ErrorMessage)`
  ${otherTextStyles};
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
          {T.translate("categoryTreeList.loading", { tree: props.label })}
        </span>
      </LoadingTree>
    );
  else if (props.error)
    return (
      <StyledErrorMessage
        message={T.translate("categoryTreeList.error", { tree: props.label })}
      />
    );
  else if (props.tree)
    return (
      <div>
        <CategoryTreeNode
          id={props.id}
          data={{ ...props.tree, tree: props.treeId }}
          depth={props.depth}
          search={props.search}
        />
      </div>
    );
  else return null;
};

export default CategoryTree;
