import styled from "@emotion/styled";
import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { forwardRef, memo } from "react";
import ReactMarkdown from "react-markdown";

import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";

const Container = styled("div")<{ active?: boolean }>`
  border-radius: ${({ theme }) => theme.borderRadius};
  display: flex;
  align-items: center;
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  padding: 0px 5px;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.black};
  box-shadow: 0.5px 0.5px 1px 0 rgb(0 0 0 / 20%), inset 0 0 0 1px #ccc;

  /* to style react-markdown */
  p {
    margin: 0;
  }
`;

const SxIconButton = styled(IconButton)`
  padding: 1px 2px 1px 5px;
`;

const SelectedItem = forwardRef<
  HTMLDivElement,
  {
    active?: boolean;
    disabled?: boolean;
    item: SelectOptionT;
    index: number;
    getSelectedItemProps: (props: {
      selectedItem: SelectOptionT;
      index: number;
    }) => any;
    removeSelectedItem: (item: SelectOptionT) => void;
  }
>(
  (
    { index, item, disabled, removeSelectedItem, getSelectedItemProps },
    ref,
  ) => {
    const label = item.selectedLabel || item.label || item.value;

    const selectedItemProps = getSelectedItemProps({
      selectedItem: item,
      index,
    });

    return (
      <Container ref={ref} {...selectedItemProps}>
        <ReactMarkdown>{String(label)}</ReactMarkdown>
        <SxIconButton
          icon={faTimes}
          disabled={disabled}
          onClick={(e) => {
            e.stopPropagation(); // otherwise the click handler on the Container overrides this
            removeSelectedItem(item);
          }}
        />
      </Container>
    );
  },
);

export default memo(SelectedItem);
