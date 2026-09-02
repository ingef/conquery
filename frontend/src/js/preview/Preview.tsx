import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useMemo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { PreviewStatistics, SecondaryId } from "../api/types";
import type { StateT } from "../app/reducers";
import { TransparentButton } from "../button/TransparentButton";
import FaIcon from "../icon/FaIcon";
import { closePreview } from "./actions";
import Charts from "./Charts";
import DiagramModal from "./DiagramModal";
import HeadlineStats from "./HeadlineStats";
import type { PreviewStateT } from "./reducer";
import ScrollBox from "./ScrollBox";
import SelectBox from "./SelectBox";
import Table from "./Table";

const fullScreen = tv({
  base: [
    "fixed top-0 left-0",
    "z-2",
    "flex flex-col",
    "gap-[15px]",
    "h-full w-full",
    "bg-bg-100",
  ],
});

const headline = tv({
  base: ["flex flex-row items-center", "gap-[30px]"],
});

const scrollBox = tv({
  base: ["flex flex-col", "gap-5", "pt-[60px] px-5 pb-5"],
});

const charts = tv({
  base: [
    "w-full",
    "bg-white",
    "p-[10px]",
    "shadow-[0_0_5px_0_rgba(0,0,0,0.2)]",
  ],
});

const chartLoadingBlocker = tv({
  base: [
    "flex items-center justify-center",
    "h-[65vh] w-full",
    "bg-white",
    "p-[10px]",
    "shadow-[0_0_5px_0_rgba(0,0,0,0.2)]",
  ],
});

// the old styles also set `width: 30px`, but FaIcon forces `width: initial
// !important`, so it never applied — only the height did
const spinnerIcon = tv({ base: "h-[30px]" });

const selectBox = tv({
  base: ["rounded", "bg-white", "shadow-[0_0_5px_0_rgba(0,0,0,0.2)]"],
});

export default function Preview() {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds,
  );
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const [selectBoxOpen, setSelectBoxOpen] = useState<boolean>(false);
  const [page, setPage] = useState<number>(0);
  const [popOver, setPopOver] = useState<PreviewStatistics | null>(null);
  const onClose = () => dispatch(closePreview());
  const statistics = preview.statisticsData;
  const idLabel = useMemo(() => {
    const primaryIdLabel = t("common.entitiesFound", { count: 2 });
    if (preview.queryData?.secondaryId) {
      const secondaryIdLabel = loadedSecondaryIds.find(
        (x) => x.id === preview.queryData?.secondaryId,
      )?.label;
      return t("preview.idLabel", { primaryIdLabel, secondaryIdLabel });
    } else {
      return `${t("queryEditor.secondaryIdStandard")} (${primaryIdLabel})`;
    }
  }, [preview.queryData, loadedSecondaryIds, t]);

  useHotkeys("esc", () => {
    if (!selectBoxOpen && !popOver) onClose();
  });

  return (
    <div className={fullScreen()}>
      <ScrollBox className={scrollBox()}>
        <div className={headline()}>
          <TransparentButton small onClick={onClose}>
            {t("common.back")}
          </TransparentButton>
          Ergebnisvorschau
          <SelectBox
            className={selectBox()}
            items={statistics?.statistics ?? ([] as PreviewStatistics[])}
            onChange={(res) => {
              const stat = statistics?.statistics.find(
                (stat) => stat.label === res.label,
              );
              setPopOver(stat ?? null);
            }}
            isOpen={selectBoxOpen}
            setIsOpen={setSelectBoxOpen}
          />
          <HeadlineStats statistics={statistics} idLabel={idLabel} />
        </div>
        {statistics ? (
          <Charts
            className={charts()}
            statistics={statistics.statistics}
            showPopup={(statistic: PreviewStatistics) => {
              setPopOver(statistic);
            }}
            page={page}
            setPage={setPage}
          />
        ) : (
          <div className={chartLoadingBlocker()}>
            <FaIcon className={spinnerIcon()} icon={faSpinner} />
          </div>
        )}
        {popOver && (
          <DiagramModal statistic={popOver} onClose={() => setPopOver(null)} />
        )}
        {preview.arrowReader &&
          preview.initialTableData &&
          preview.queryData && (
            <Table
              arrowReader={preview.arrowReader}
              initialTableData={preview.initialTableData}
              queryData={preview.queryData}
            />
          )}
      </ScrollBox>
    </div>
  );
}
