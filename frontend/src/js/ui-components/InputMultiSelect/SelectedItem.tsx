import styled from "@emotion/styled";
import { forwardRef } from "react";
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
    option: SelectOptionT;
    onRemoveClick: () => void;
  }
>(({ option, disabled, onRemoveClick, ...rest }, ref) => {
  const label = option.selectedLabel || option.label || option.value;

  return (
    <Container ref={ref} {...rest}>
      <ReactMarkdown>{String(label)}</ReactMarkdown>
      <SxIconButton icon="times" disabled={disabled} onClick={onRemoveClick} />
    </Container>
  );
});

export default SelectedItem;
