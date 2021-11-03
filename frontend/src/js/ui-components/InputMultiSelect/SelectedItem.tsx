import styled from "@emotion/styled";
import { forwardRef } from "react";

import type { SelectOptionT } from "../../api/types";
import IconButton from "../../button/IconButton";

const Container = styled("div")<{ active?: boolean }>`
  border-radius: ${({ theme }) => theme.borderRadius};
  display: flex;
  align-items: center;
  background-color: ${({ theme }) => theme.col.grayVeryLight};
  padding: 1px 5px;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.black};
  box-shadow: 0 0 1px 0 rgba(0, 0, 0, 0.8);
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
  return (
    <Container ref={ref} {...rest}>
      <span>{option.label}</span>
      <SxIconButton icon="times" disabled={disabled} onClick={onRemoveClick} />
    </Container>
  );
});

export default SelectedItem;
