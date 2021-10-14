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
`;

const SxIconButton = styled(IconButton)`
  padding: 1px 2px 1px 5px;
`;

const SelectedItem = forwardRef<
  HTMLDivElement,
  {
    active?: boolean;
    option: SelectOptionT;
    onRemoveClick: () => void;
  }
>(({ option, active, onRemoveClick, ...rest }, ref) => {
  return (
    <Container ref={ref} active={active} {...rest}>
      <span>{option.label}</span>
      <SxIconButton icon="times" onClick={onRemoveClick} />
    </Container>
  );
});

export default SelectedItem;
