import styled from "@emotion/styled";
import { FC } from "react";

const ProgressText = styled("div")`
  font-size: ${({ theme }) => theme.font.lg};
  margin-right: 10px;
  font-weight: 700;
  color: ${({ theme }) => theme.col.blueGray};
`;

// progress is between 0 and 1
interface Props {
  progress: number;
}

const QueryRunningProgress: FC<Props> = ({ progress }) => {
  return <ProgressText>{Math.round(progress * 100)} %</ProgressText>;
};

export default QueryRunningProgress;
