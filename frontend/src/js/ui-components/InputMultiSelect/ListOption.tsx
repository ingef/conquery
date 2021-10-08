import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { forwardRef } from "react";

const Container = styled("div")<{ active?: boolean }>`
  padding: 3px 8px;
  cursor: pointer;
  color: ${({ theme }) => theme.col.black};

  transition: background-color ${({ theme }) => theme.transitionTime};

  ${({ active, theme }) =>
    active &&
    css`
      background-color: ${theme.col.blueGrayVeryLight};
    `};
`;

const ListOption = forwardRef<HTMLDivElement, { active?: boolean }>(
  (props, ref) => {
    return <Container {...props} ref={ref} />;
  },
);

export default ListOption;
