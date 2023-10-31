import styled from "@emotion/styled"
import { PreviewStatistics } from "../api/types";
import Diagram from "./Diagram";
import { useState } from "react";
import { faArrowLeft, faArrowRight } from "@fortawesome/free-solid-svg-icons";
import IconButton from "../button/IconButton";

const Root = styled("div")`
`

const SxDiagram = styled(Diagram)`
  width: 100px;
  padding: 5px;
  margin-right: 15px;
`;
const DirectionSelector = styled("div")`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 5px;
`;


type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
}

export default function Charts({ statistics, className }: ChartProps) {
  const [index, setIndex] = useState<number>(0);

  return (
    <Root className={className}>
      {statistics.slice(index*4, (index+1)*4).map((statistic) => {
        return (
          <div>
            <SxDiagram stat={statistic} />
          </div>
        )
      })}
      <DirectionSelector>
        <IconButton icon={faArrowLeft} onClick={() => setIndex(index - 1)} disabled={index===0} />
        <IconButton icon={faArrowRight} onClick={() => setIndex(index + 1)} disabled={(index+1)*4 >= statistics.length} />
      </DirectionSelector>
    </Root>
  )
}
