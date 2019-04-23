// @flow

import React from "react";
import styled from "@emotion/styled";
import { findDOMNode } from "react-dom";
import { DragSource } from "react-dnd";
import Highlighter from "react-highlight-words";

import { AdditionalInfoHoverable } from "../tooltip";
import { isEmpty } from "../common/helpers";
import { dndTypes } from "../common/constants";

import FaIcon from "../icon/FaIcon";
import { type AdditionalInfoHoverableNodeType } from "../tooltip/AdditionalInfoHoverable";
import { type DraggedNodeType } from "../standard-query-editor/types";
import { type SearchType } from "./reducer";

const Root = styled("div")`
  position: relative; // Needed to fix a drag & drop issue in Safari
  cursor: pointer;
  padding: 0 15px 0 15px;
  margin: 1px 0;
  padding-left: ${({ depth }) => depth * 15 + "px"};
  display: inline-block;
`;

const Text = styled("p")`
  user-select: none;
  border-radius: ${({ theme }) => theme.borderRadius};
  margin: 0;
  padding: 0 14px;
  line-height: 20px;
  color: ${({ theme, zero }) => (zero ? theme.col.red : theme.col.black)};

  background-color: ${({ theme, open }) =>
    open ? theme.col.grayVeryLight : "transparent"};

  &:hover {
    background-color: ${({ theme }) => theme.col.blueGrayVeryLight};
    opacity: ${({ open }) => (open ? "0.9" : "1")};
  }
`;

const StyledFaIcon = styled(FaIcon)`
  padding-right: 7px;
  width: 20px;
`;

const Caret = styled(FaIcon)`
  width: 12px;
`;

const ResultsNumber = styled("span")`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  padding: 2px 4px;
  margin-right: 5px;
  font-size: ${({ theme }) => theme.font.xs};
  border-radius: 3px;
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
`;

type PropsType = {
  node: AdditionalInfoHoverableNodeType & {
    label: string,
    description?: string,
    matchingEntries?: number
  },
  open: boolean,
  depth: number,
  active?: boolean,
  onTextClick?: Function,
  createQueryElement: () => DraggedNodeType,
  connectDragSource: Function,
  search?: SearchType
};

function shouldShowNumber(search, node) {
  return (
    search.result &&
    search.result[node.id] > 0 &&
    (node.children && node.children.some(child => search.result[child] > 0))
  );
}

// Has to be a class because of https://github.com/react-dnd/react-dnd/issues/530
class CategoryTreeNodeTextContainer extends React.Component {
  render() {
    const {
      node,
      depth,
      search,
      active,
      open,
      connectDragSource,
      onTextClick
    } = this.props;

    const zeroEntries =
      !isEmpty(node.matchingEntries) && node.matchingEntries === 0;
    const description = ` - ${node.description}`;
    const showNumber = shouldShowNumber(search, node);
    const hasChildren = !!node.children && node.children.length > 0;

    return (
      <Root
        ref={instance => {
          // Don't allow dragging with inactive elements
          if (active !== false) {
            connectDragSource(instance);
          }
        }}
        onClick={onTextClick}
        depth={depth}
      >
        <Text open={open} zero={zeroEntries}>
          {hasChildren && (
            <Caret active icon={!!open ? "caret-down" : "caret-right"} />
          )}
          {hasChildren && (
            <StyledFaIcon active icon={!!open ? "folder-open" : "folder"} />
          )}
          {showNumber && (
            <ResultsNumber>{search.result[node.id]}</ResultsNumber>
          )}
          <span>
            {search.words ? (
              <Highlighter
                searchWords={search.words}
                autoEscape={true}
                textToHighlight={node.label}
              />
            ) : (
              node.label
            )}
          </span>
          {search.words && node.description ? (
            <Highlighter
              searchWords={search.words}
              autoEscape={true}
              textToHighlight={description}
            />
          ) : (
            node.description && description
          )}
        </Text>
      </Root>
    );
  }
}

/**
 * Implements the drag source contract.
 */
const nodeSource = {
  beginDrag(props: PropsType, monitor, component): DraggedNodeType {
    const { width, height } = findDOMNode(component).getBoundingClientRect();

    return {
      width,
      height,
      ...props.createQueryElement()
    };
  }
};

/**
 * Specifies the props to inject into your component.
 */
const collect = (connect, monitor) => ({
  connectDragSource: connect.dragSource(),
  isDragging: monitor.isDragging()
});

const DraggableCategoryTreeNodeTextContainer = DragSource(
  dndTypes.CATEGORY_TREE_NODE,
  nodeSource,
  collect
)(CategoryTreeNodeTextContainer);

export default AdditionalInfoHoverable(DraggableCategoryTreeNodeTextContainer);
