import styled from "@emotion/styled";
import { PreviewStatisticsResponse } from "../api/types";
import TooltipEntries from "../tooltip/TooltipEntries";

const Root = styled("div")`
  padding: 10px;
  align-self: right;
  margin-left: auto;
`;

const SxTooltipEntries = styled(TooltipEntries)`
  display: flex;
  flex-direction: row;
  gap: 12px 12px;
  margin: auto;
`;

export type HeadlineStatsProps = {
  statistics: PreviewStatisticsResponse | null;
};

export default function HeadlineStats({ statistics }: HeadlineStatsProps) {
  return (
    <Root>
      <SxTooltipEntries
        matchingEntities={statistics?.entities}
        matchingEntries={statistics?.total}
        dateRange={statistics?.dateRange}
      />
    </Root>
  );
}
