import { tv } from "tailwind-variants";
import type { PreviewStatistics } from "../api/types";
import Diagram from "./Diagram";

const root = tv({
  base: ["grid grid-cols-2", "gap-5", "overflow-x-hidden"],
});

const diagram = tv({ base: "h-[27vh]" });

export const DIAGRAMS_PER_PAGE = 4;

// the axis keeps its height for the chart when category labels run long
const MAX_LABEL_LENGTH = 24;

export default function Charts({
  statistics,
  showPopup,
  page,
}: {
  statistics: PreviewStatistics[];
  showPopup: (statistic: PreviewStatistics) => void;
  page: number;
}) {
  const diagramsOnPage = statistics.slice(
    page * DIAGRAMS_PER_PAGE,
    (page + 1) * DIAGRAMS_PER_PAGE,
  );

  return (
    <div className={root()}>
      {diagramsOnPage.map((statistic) => (
        <Diagram
          key={statistic.label}
          className={diagram()}
          stat={statistic}
          maxLabelLength={MAX_LABEL_LENGTH}
          onClick={() => showPopup(statistic)}
        />
      ))}
    </div>
  );
}
