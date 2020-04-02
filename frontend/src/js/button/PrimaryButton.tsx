import styled from "@emotion/styled";

import BasicButton from "./BasicButton";

export default styled(BasicButton)`
  color: white;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-width: 1px;
  border-style: solid;
  border-color: ${({ theme }) => theme.col.blueGrayDark};
  background-clip: padding-box;
  border: none;

  &:hover {
    opacity: 0.9;
  }
`;
