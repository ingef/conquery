// @flow

import React from "react";
import styled from "@emotion/styled";

import {
  type TreeNodeIdType,
  type InfoType,
  type DateRangeType,
  type NodeType
} from "../common/types/backend";
import { type DraggedNodeType } from "../standard-query-editor/types";
import { type SearchType } from "./reducer";

import { getConceptById } from "./globalTreeStoreHelper";
import Openable from "./Openable";
import CategoryTreeNodeTextContainer from "./CategoryTreeNodeTextContainer";
import { isNodeInSearchResult } from "./selectors";

const Root = styled("div")`
  font-size: ${({ theme }) => theme.font.sm};
`;

// Concept data that is necessary to display tree nodes. Includes additional infos
// for the tooltip as well as the id of the corresponding tree
type TreeNodeData = {
  label: string,
  description: string,
  active: boolean,
  matchingEntries: number,
  dateRange: DateRangeType,
  additionalInfos: Array<InfoType>,
  children: Array<TreeNodeIdType>,

  tree: TreeNodeIdType
};

type PropsType = {
  id: TreeNodeIdType,
  data: TreeNodeData,
  depth: number,
  open: boolean,
  search?: SearchType,
  onToggleOpen: () => void
};

const selectTreeNodeData = (concept: NodeType, tree: TreeNodeIdType) => ({
  label: concept.label,
  description: concept.description,
  active: concept.active,
  matchingEntries: concept.matchingEntries,
  dateRange: concept.dateRange,
  additionalInfos: concept.additionalInfos,
  children: concept.children,

  tree
});

class CategoryTreeNode extends React.Component<PropsType> {
  _onToggleOpen() {
    if (!this.props.data.children) return;

    this.props.onToggleOpen();
  }

  render() {
    const { id, data, depth, open, search } = this.props;

    if (!search.showMismatches) {
      const shouldRender = isNodeInSearchResult(id, data.children, search);

      if (!shouldRender) return null;
    }

    const isOpen = open || search.allOpen;

    return (
      <Root>
        <CategoryTreeNodeTextContainer
          node={{
            id,
            label: data.label,
            description: data.description,
            matchingEntries: data.matchingEntries,
            dateRange: data.dateRange,
            additionalInfos: data.additionalInfos,
            children: data.children
          }}
          createQueryElement={(): DraggedNodeType => {
            const { tables, selects } = getConceptById(data.tree);
            const description = data.description
              ? { description: data.description }
              : {};

            return {
              ids: [id],
              ...description,
              label: data.label,
              tables,
              selects,
              tree: data.tree
            };
          }}
          open={isOpen}
          depth={depth}
          active={data.active}
          onTextClick={this._onToggleOpen.bind(this)}
          search={search}
        />
        {!!data.children && isOpen && (
          <>
            {data.children.map((childId, i) => {
              const child = getConceptById(childId);

              return child ? (
                <OpenableCategoryTreeNode
                  key={i}
                  id={childId}
                  data={selectTreeNodeData(child, data.tree)}
                  depth={this.props.depth + 1}
                  search={this.props.search}
                />
              ) : null;
            })}
          </>
        )}
      </Root>
    );
  }
}

const OpenableCategoryTreeNode = Openable(CategoryTreeNode);

export default OpenableCategoryTreeNode;
