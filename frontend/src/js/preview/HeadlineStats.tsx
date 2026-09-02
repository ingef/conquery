import { tv } from "tailwind-variants";
import type { PreviewStatisticsResponse } from "../api/types";
import TooltipEntries from "../tooltip/TooltipEntries";

// the old styles also declared `align-self: right` — an invalid value, never applied
const root = tv({ base: ["ml-auto", "p-[10px]"] });

const tooltipEntries = tv({
  base: ["flex flex-row", "gap-3", "m-auto"],
});

export type HeadlineStatsProps = {
  statistics: PreviewStatisticsResponse | null;
  idLabel: string;
};

export default function HeadlineStats({
  statistics,
  idLabel,
}: HeadlineStatsProps) {
  return (
    <div className={root()}>
      <TooltipEntries
        className={tooltipEntries()}
        matchingEntities={statistics?.entities}
        matchingEntries={statistics?.total}
        dateRange={statistics?.dateRange}
        idLabel={idLabel}
      />
    </div>
  );
}
