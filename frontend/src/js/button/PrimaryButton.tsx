import styled from "@emotion/styled";

import BasicButton from "./BasicButton";

export default styled(BasicButton)`
  color: white;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  background-clip: padding-box;
  border: 1px solid ${({ theme }) => theme.col.blueGrayDark};

  &:hover {
    opacity: 0.9;
  }
`;
