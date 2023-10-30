import styled from "@emotion/styled"
import { PreviewStatistics } from "../api/types";
import Diagram from "./Diagram";

const Root = styled("div")`
`

type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
}

export default function Charts({ statistics, className }: ChartProps) {
  console.log(statistics);

  return (
    <Root className={className}>

      {statistics.map((statistic, i) => {
        return (
          <div key={i}>
            <Diagram stat={statistic} />
          </div>
        )
      })}

    </Root>
  )
}
