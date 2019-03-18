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
  margin: 2px 0;
  padding-left: ${({ depth }) => depth * 15 + "px"};
`;

const Text = styled("p")`
  user-select: none;
  border-radius: ${({ theme }) => theme.borderRadius};
  margin: 0;
  padding: 0 14px;
  line-height: 20px;
  color: ${({ theme, zero }) => (zero ? theme.col.red : theme.col.black)};

  background-color: ${({ theme, open }) =>
    open ? theme.col.blueGrayVeryLight : "transparent"};

  &:hover {
    background-color: ${({ theme, open }) =>
      open
        ? `rgba(${theme.col.blueGrayVeryLight}, 0.8)`
        : theme.col.blueGrayVeryLight};
  }
`;

type PropsType = {
  node: AdditionalInfoHoverableNodeType & {
    label: string,
    hasChildren: boolean,
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

const StyledFaIcon = styled(FaIcon)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  padding-right: 7px;
`;

// Has to be a class because of https://github.com/react-dnd/react-dnd/issues/530
class CategoryTreeNodeTextContainer extends React.Component {
  render() {
    const { props } = this;
    const zeroEntries =
      !isEmpty(props.node.matchingEntries) && props.node.matchingEntries === 0;
    const searching = props.search && props.search.searching;
    const description = ` - ${props.node.description}`;

    return (
      <Root
        ref={instance => {
          // Don't allow dragging with inactive elements
          if (props.active !== false) {
            props.connectDragSource(instance);
          }
        }}
        onClick={props.onTextClick}
        depth={props.depth}
      >
        <Text open={props.open} zero={zeroEntries}>
          {props.node.hasChildren && (
            <StyledFaIcon icon={!!props.open ? "folder-open" : "folder"} />
          )}
          <span>
            {searching ? (
              <Highlighter
                searchWords={props.search && props.search.words}
                autoEscape={true}
                textToHighlight={props.node.label}
              />
            ) : (
              props.node.label
            )}
          </span>
          {searching && props.node.description ? (
            <Highlighter
              searchWords={props.search && props.search.words}
              autoEscape={true}
              textToHighlight={description}
            />
          ) : (
            props.node.description && description
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
