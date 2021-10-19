import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";

interface Props {
  active?: boolean;
  disabled?: boolean;
}

const Container = styled("div")<Props>`
  padding: 3px 8px;
  cursor: pointer;
  color: ${({ theme }) => theme.col.black};

  transition: background-color ${({ theme }) => theme.transitionTime};

  ${({ active, theme }) =>
    active &&
    css`
      background-color: ${theme.col.blueGrayVeryLight};
    `};
  ${({ disabled }) =>
    disabled &&
    css`
      opacity: 0.5;
      cursor: not-allowed;
    `};
`;

const SelectListOption = forwardRef<HTMLDivElement, Props>((props, ref) => {
  return <Container {...props} ref={ref} />;
});

export default SelectListOption;
