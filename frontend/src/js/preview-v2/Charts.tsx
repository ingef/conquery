import styled from "@emotion/styled"
import { PreviewStatistics } from "../api/types";

const Root = styled("div")`
`

type ChartProps = {
  statistics: PreviewStatistics[];
}

export default function Charts({statistics}: ChartProps) {
  return (
    <Root>Charts</Root>
  )
}
