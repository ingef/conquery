import styled from "@emotion/styled";

const ProgressText = styled("div")`
  font-size: ${({ theme }) => theme.font.lg};
  margin-right: 10px;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGray};
`;

// progress is between 0 and 1

const QueryRunningProgress = ({ progress }: { progress: number }) => {
  return <ProgressText>{Math.round(progress * 100)} %</ProgressText>;
};

export default QueryRunningProgress;
