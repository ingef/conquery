import styled from "@emotion/styled";

const StatsSubline = styled("h4")`
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.black};
  font-weight: 400;
  margin: 0 0 12px;
`;

export default StatsSubline;
