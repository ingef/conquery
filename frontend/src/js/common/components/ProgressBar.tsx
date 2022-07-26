import styled from "@emotion/styled";
import { memo } from "react";

const Bar = styled("div")`
  width: 100%;
  height: 7px;
  background-color: #ccc;
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
`;

const BarProgress = styled("div")`
  height: 100%;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
`;

interface Props {
  className?: string;
  donePercent: number;
}

const ProgressBar = ({ className, donePercent }: Props) => {
  return (
    <Bar className={className}>
      <BarProgress style={{ width: `${donePercent}%` }} />
    </Bar>
  );
};

export default memo(ProgressBar);
