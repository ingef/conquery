import styled from "@emotion/styled";
import { useEffect, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector, useStore } from "react-redux";

import { StateT } from "../app/reducers";

import { PreviewStatistics } from "../api/types";
import { TransparentButton } from "../button/TransparentButton";
import FaIcon from "../icon/FaIcon";
import PreviewInfo from "../preview/PreviewInfo";

import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { toggleDragHandles } from "../pane/actions";
import Charts from "./Charts";
import DiagramModal from "./DiagramModal";
import HeadlineStats from "./HeadlineStats";
import ScrollBox from "./ScrollBox";
import SelectBox from "./SelectBox";
import Table from "./Table";
import { closePreview } from "./actions";
import { PreviewStateT } from "./reducer";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: ${({ theme }) => theme.col.bgAlt};
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 15px;
`;

const Headline = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 30px;
`;

const SxScrollBox = styled(ScrollBox)`
  padding: 60px 20px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
`;

const SxCharts = styled(Charts)`
  width: 100%;
  background-color: white;
  padding: 10px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
`;

const SxChartLoadingBlocker = styled("div")`
  width: 100%;
  background-color: white;
  padding: 10px;
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  align-items: center;
  height: 65vh;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const SxFaIcon = styled(FaIcon)`
  width: 30px;
  height: 30px;
`;

const SxSelectBox = styled(SelectBox)`
  box-shadow: 0 0 5px 0 rgba(0, 0, 0, 0.2);
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

export default function Preview() {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const [selectBoxOpen, setSelectBoxOpen] = useState<boolean>(false);
  const [page, setPage] = useState<number>(0);
  const [popOver, setPopOver] = useState<PreviewStatistics | null>(null);
  const onClose = () => dispatch(closePreview());
  const statistics = preview.statisticsData;

  useHotkeys("esc", () => {
    if (!selectBoxOpen && !popOver) onClose();
  });

  const store = useStore();
  useEffect(() => {
    if (!(store.getState() as StateT).panes.disableDragHandles) {
      dispatch(toggleDragHandles());
      return () => {
        dispatch(toggleDragHandles());
      };
    }
  }, [preview.statisticsData, dispatch, store]);

  return (
    <FullScreen>
      <SxScrollBox>
        <PreviewInfo
          rawPreviewData={[]}
          columns={[]}
          onClose={onClose}
          minDate={new Date()}
          maxDate={new Date()}
        />
        <Headline>
          <TransparentButton small onClick={onClose}>
            {t("common.back")}
          </TransparentButton>
          Ergebnisvorschau
          <SxSelectBox
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
          <HeadlineStats statistics={statistics} />
        </Headline>
        {statistics ? (
          <SxCharts
            statistics={statistics.statistics}
            showPopup={(statistic: PreviewStatistics) => {
              setPopOver(statistic);
            }}
            page={page}
            setPage={setPage}
          />
        ) : (
          <SxChartLoadingBlocker>
            <SxFaIcon icon={faSpinner} />
          </SxChartLoadingBlocker>
        )}
        {popOver && (
          <DiagramModal statistic={popOver} onClose={() => setPopOver(null)} />
        )}
        {preview.tableData && preview.queryData && (
          <Table data={preview.tableData} queryData={preview.queryData} />
        )}
      </SxScrollBox>
    </FullScreen>
  );
}
