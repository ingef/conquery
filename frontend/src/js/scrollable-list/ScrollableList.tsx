import * as React from "react";
import styled from "@emotion/styled";
import { css } from "@emotion/core";
import ReactList from "react-list";

type PropsType = {
  items: React.Node[];
  maxVisibleItems?: number;
  fullWidth?: boolean;
  minWidth?: boolean;
};

const Root = styled("div")`
  overflow-x: none;
  overflow-y: auto;
  // If the number of visible items is specified here,
  // make an additional element half-visible at the end to indicate
  // that the list is scrollable
  max-height: ${({ maxVisibleItems }) => (maxVisibleItems + 0.5) * 34}px;
  max-width: 340px;
  border-radius: 2px;
  border: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  color: ${({ theme }) => theme.col.black};

  ${({ fullWidth }) =>
    fullWidth &&
    css`
      width: 100%;
      max-width: 100%;
    `};
`;

const Item = styled("div")`
  line-height: 34px;
  padding-left: 10px;
  padding-right: 10px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ScrollableList = (props: PropsType) => {
  const renderItem = (index, key) => {
    return (
      <Item key={key} className="scrollable-list-item">
        {props.items[index]}
      </Item>
    );
  };

  return (
    <Root maxVisibleItems={props.maxVisibleItems} fullWidth={!!props.fullWidth}>
      <ReactList
        itemRenderer={renderItem}
        length={props.items ? props.items.length : 0}
        type="uniform"
      />
    </Root>
  );
};

export default ScrollableList;
