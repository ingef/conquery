import { ArrowLeftIcon, ArrowRightIcon } from "lucide-react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { PreviewStatistics } from "../api/types";
import { Button } from "../ui-components/Button";
import { C2 } from "../ui-components/Typography";
import Diagram from "./Diagram";

const root = tv({ base: ["flex flex-col", "gap-4"] });

const diagramContainer = tv({
  base: ["grid grid-cols-2", "gap-5", "overflow-x-hidden"],
});

const diagram = tv({ base: "h-[27vh]" });

const pagination = tv({
  base: ["flex items-center justify-center", "gap-2"],
});

type ChartProps = {
  statistics: PreviewStatistics[];
  showPopup: (statistic: PreviewStatistics) => void;
  page: number;
  setPage: (page: number) => void;
};

const DIAGRAMS_PER_PAGE = 4;

export default function Charts({
  statistics,
  showPopup,
  page,
  setPage,
}: ChartProps) {
  const { t } = useTranslation();
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
    <div className={root()}>
      <div className={diagramContainer()}>
        {diagramsOnPage.map((statistic) => (
          <Diagram
            key={statistic.label}
            className={diagram()}
            stat={statistic}
            onClick={() => showPopup(statistic)}
          />
        ))}
      </div>
      <div className={pagination()}>
        <Button
          intent="tertiary"
          aria-label={t("preview.previousPage")}
          onPress={() => updatePage(-1)}
          isDisabled={page === 0}
        >
          <ArrowLeftIcon />
        </Button>
        <C2 as="span">
          {t("preview.page")} {page + 1}/{maxPage}
        </C2>
        <Button
          intent="tertiary"
          aria-label={t("preview.nextPage")}
          onPress={() => updatePage(1)}
          isDisabled={page === maxPage - 1}
        >
          <ArrowRightIcon />
        </Button>
      </div>
    </div>
  );
}
