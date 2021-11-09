import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { ReactNode, useEffect } from "react";
import ReactList from "react-list";

interface PropsType {
  items: ReactNode[];
  maxVisibleItems: number;
  fullWidth?: boolean;
}

// With the number of visible items specified here,
// make an additional element half-visible at the end to indicate
// that the list is scrollable
const Root = styled("div")<{ maxVisibleItems: number; fullWidth?: boolean }>`
  max-height: ${({ maxVisibleItems }) => (maxVisibleItems + 0.5) * 34}px;

  overflow-x: none;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
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
  line-height: 24px;
  padding-left: 10px;
  padding-right: 10px;
  border-bottom: 1px solid ${({ theme }) => theme.col.grayVeryLight};
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: ${({ theme }) => theme.font.sm};
`;

const ScrollableList = ({ items, maxVisibleItems, fullWidth }: PropsType) => {
  const renderItem = (index: number, key: string | number) => {
    return (
      <Item key={key} className="scrollable-list-item">
        {items[index]}
      </Item>
    );
  };

  return (
    <Root maxVisibleItems={maxVisibleItems} fullWidth={!!fullWidth}>
      <ReactList
        itemRenderer={renderItem}
        length={items ? items.length : 0}
        type="uniform"
      />
    </Root>
  );
};

export default ScrollableList;
