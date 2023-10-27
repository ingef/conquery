import styled from "@emotion/styled"
import { PreviewStatistics } from "../api/types";

const Root = styled("div")`
`

type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
}

export default function Charts({ statistics, className }: ChartProps) {
  

  return (
    <Root className={className}>

      {statistics.map((statistic, i) => {
        return (
          <div key={i}>
            {statistic.name}
          </div>
        )
      })}

    </Root>
  )
}
