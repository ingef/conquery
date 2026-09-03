import { faArrowLeft, faArrowRight } from "@fortawesome/free-solid-svg-icons";
import { t } from "i18next";
import { useHotkeys } from "react-hotkeys-hook";
import { tv } from "tailwind-variants";
import type { PreviewStatistics } from "../api/types";
import IconButton from "../button/IconButton";
import Diagram from "./Diagram";

const diagram = tv({
  base: ["h-[27vh]", "mr-[15px]", "p-[5px]"],
});

// the old styles also declared `font-size: 24` on the buttons — a unitless
// value, invalid css, never applied
const directionSelector = tv({
  base: [
    "col-start-1 col-end-3 row-start-3",
    "flex flex-row items-center justify-center",
    "mb-[5px]",
    "px-[100px]",
  ],
});

const diagramContainer = tv({
  base: ["grid grid-cols-2", "gap-[5px]", "overflow-x-hidden"],
});

type ChartProps = {
  statistics: PreviewStatistics[];
  className?: string;
  showPopup: (statistic: PreviewStatistics) => void;
  page: number;
  setPage: (page: number) => void;
};

const DIAGRAMS_PER_PAGE = 4;

export default function Charts({
  statistics,
  className,
  showPopup,
  page,
  setPage,
}: ChartProps) {
  const diagramsOnPage = statistics.slice(
    page * DIAGRAMS_PER_PAGE,
    (page + 1) * DIAGRAMS_PER_PAGE,
  );
  const maxPage = Math.ceil(statistics.length / DIAGRAMS_PER_PAGE);

  const updatePage = (change: number) => {
    const newValue = page + change;
    if (newValue >= 0 && newValue < maxPage) {
      setPage(newValue);
    }
  };

  useHotkeys("left", () => updatePage(-1), [page]);
  useHotkeys("right", () => updatePage(1), [page]);

  return (
    <div className={className}>
      <div className={diagramContainer()}>
        {diagramsOnPage.map((statistic) => {
          return (
            <div key={statistic.label}>
              <Diagram
                className={diagram()}
                stat={statistic}
                onClick={() => showPopup(statistic)}
              />
            </div>
          );
        })}
      </div>
      <div className={directionSelector()}>
        <IconButton
          icon={faArrowLeft}
          onClick={() => updatePage(-1)}
          disabled={page === 0}
        />
        <span>
          {t("preview.page")} {page + 1}/
          {Math.ceil(statistics.length / DIAGRAMS_PER_PAGE)}
        </span>
        <IconButton
          icon={faArrowRight}
          onClick={() => updatePage(1)}
          disabled={page === maxPage - 1}
        />
      </div>
    </div>
  );
}
