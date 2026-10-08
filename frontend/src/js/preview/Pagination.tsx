import { ArrowLeftIcon, ArrowRightIcon } from "lucide-react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
import { C2 } from "../ui-components/Typography";

const root = tv({ base: ["flex items-center justify-center", "gap-2"] });

export default function Pagination({
  page,
  pageCount,
  setPage,
}: {
  page: number;
  pageCount: number;
  setPage: (page: number) => void;
}) {
  const { t } = useTranslation();

  const updatePage = (change: number) => {
    const newValue = page + change;
    if (newValue >= 0 && newValue < pageCount) {
      setPage(newValue);
    }
  };

  useHotkeys("left", () => updatePage(-1), [page]);
  useHotkeys("right", () => updatePage(1), [page]);

  return (
    <div className={root()}>
      <Button
        intent="tertiary"
        aria-label={t("preview.previousPage")}
        onPress={() => updatePage(-1)}
        isDisabled={page === 0}
      >
        <ArrowLeftIcon />
      </Button>
      <C2 as="span">
        {t("preview.page")} {page + 1}/{pageCount}
      </C2>
      <Button
        intent="tertiary"
        aria-label={t("preview.nextPage")}
        onPress={() => updatePage(1)}
        isDisabled={page === pageCount - 1}
      >
        <ArrowRightIcon />
      </Button>
    </div>
  );
}
