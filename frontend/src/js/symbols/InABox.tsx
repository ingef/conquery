import styled from "@emotion/styled";

export const InABox = styled.div`
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 3px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  width: 24px;
`;
