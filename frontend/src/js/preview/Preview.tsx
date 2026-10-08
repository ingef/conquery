import {
  ChevronDownIcon,
  ChevronLeftIcon,
  LoaderCircleIcon,
} from "lucide-react";
import { type ReactNode, useMemo, useState } from "react";
import { MenuTrigger } from "react-aria-components";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { PreviewStatistics, SecondaryId } from "../api/types";
import type { StateT } from "../app/reducers";
import MatchingStats from "../info-pane/MatchingStats";
import { Button } from "../ui-components/Button";
import { Card } from "../ui-components/Card";
import { Menu, MenuItem } from "../ui-components/Menu";
import { C3, H3 } from "../ui-components/Typography";
import { closePreview } from "./actions";
import Charts from "./Charts";
import DiagramModal from "./DiagramModal";
import type { PreviewStateT } from "./reducer";
import ScrollBox from "./ScrollBox";
import Table from "./Table";

const fullScreen = tv({
  base: ["fixed top-0 left-0", "z-2", "h-full w-full", "bg-bg-100"],
});

const content = tv({
  base: ["flex flex-col", "gap-5", "px-5 pt-[55px] pb-5"],
});

const topRow = tv({ base: ["flex items-center", "gap-[30px]"] });

const section = tv({ base: ["flex flex-col", "gap-[10px]"] });

const sectionHeader = tv({
  base: ["flex items-start justify-between", "gap-5"],
});

// the stats stand beside the charts, like the info pane beside the panes
const statistics = tv({
  base: ["grid grid-cols-[auto_minmax(0,1fr)]", "gap-5", "items-start"],
});

const loading = tv({
  base: ["flex items-center justify-center", "h-[65vh]"],
});

const SectionHeader = ({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children?: ReactNode;
}) => (
  <div className={sectionHeader()}>
    <div className="min-w-0">
      <H3>{title}</H3>
      <C3 tone="muted">{subtitle}</C3>
    </div>
    {children}
  </div>
);

export default function Preview() {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const loadedSecondaryIds = useSelector<StateT, SecondaryId[]>(
    (state) => state.conceptTrees.secondaryIds,
  );
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const [page, setPage] = useState<number>(0);
  const [popOver, setPopOver] = useState<PreviewStatistics | null>(null);
  const onClose = () => dispatch(closePreview());
  const stats = preview.statisticsData;
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
    if (!popOver) onClose();
  });

  return (
    <div className={fullScreen()}>
      <ScrollBox className="h-full">
        <div className={content()}>
          <div className={topRow()}>
            <Button intent="secondary" onPress={onClose}>
              <ChevronLeftIcon />
              {t("common.back")}
            </Button>
            <div className="min-w-0">
              <H3 truncate>{preview.queryData?.label}</H3>
              <C3 tone="muted">{t("preview.headline")}</C3>
            </div>
          </div>
          <section className={section()}>
            <SectionHeader
              title={t("preview.statisticsHeadline")}
              subtitle={t("preview.statisticsSubline")}
            >
              {stats && (
                <MenuTrigger>
                  <Button intent="secondary">
                    {t("preview.statisticsHeadline")}
                    <ChevronDownIcon />
                  </Button>
                  <Menu
                    aria-label={t("preview.statisticsHeadline")}
                    placement="bottom end"
                    onAction={(key) => {
                      const stat = stats.statistics.find(
                        (stat) => stat.label === key,
                      );
                      setPopOver(stat ?? null);
                    }}
                  >
                    {stats.statistics.map((stat) => (
                      <MenuItem key={stat.label} id={stat.label}>
                        {stat.label}
                      </MenuItem>
                    ))}
                  </Menu>
                </MenuTrigger>
              )}
            </SectionHeader>
            <div className={statistics()}>
              <div className="max-w-[300px]">
                <MatchingStats
                  matchingEntities={stats?.entities}
                  matchingEntries={stats?.total}
                  dateRange={stats?.dateRange}
                  idLabel={idLabel}
                />
              </div>
              <Card>
                {stats ? (
                  <Charts
                    statistics={stats.statistics}
                    showPopup={(statistic: PreviewStatistics) => {
                      setPopOver(statistic);
                    }}
                    page={page}
                    setPage={setPage}
                  />
                ) : (
                  <div className={loading()}>
                    <LoaderCircleIcon className="size-[30px]" />
                  </div>
                )}
              </Card>
            </div>
          </section>
          {popOver && (
            <DiagramModal
              statistic={popOver}
              onClose={() => setPopOver(null)}
            />
          )}
          {preview.arrowReader &&
            preview.initialTableData?.value &&
            preview.queryData && (
              <section className={section()}>
                <SectionHeader
                  title={t("preview.previewHeadline")}
                  subtitle={t("preview.previewSubline", {
                    count: preview.initialTableData.value.numRows,
                  })}
                />
                <Card>
                  <Table
                    arrowReader={preview.arrowReader}
                    initialTableData={preview.initialTableData}
                    queryData={preview.queryData}
                  />
                </Card>
              </section>
            )}
        </div>
      </ScrollBox>
    </div>
  );
}
