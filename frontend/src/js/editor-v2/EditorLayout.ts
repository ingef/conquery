import styled from "@emotion/styled";

export const Grid = styled("div")`
  flex-grow: 1;
  display: grid;
  gap: 3px;
  height: 100%;
  width: 100%;
  place-items: center;
  overflow: auto;
`;

export const Connector = styled("span")`
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.sm};
  color: black;

  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 0px 5px;
  display: flex;
  justify-content: center;
  align-items: center;
`;
