import styled from "@emotion/styled";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useEffect, useMemo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { usePreviewStatistics } from "../api/api";
import { PreviewStatistics, PreviewStatisticsResponse } from "../api/types";
import { TransparentButton } from "../button/TransparentButton";
import FaIcon from "../icon/FaIcon";
import PreviewInfo from "../preview/PreviewInfo";
import { setMessage } from "../snack-message/actions";
import { SnackMessageType } from "../snack-message/reducer";

import Charts from "./Charts";
import DiagramModal from "./DiagramModal";
import HeadlineStats from "./HeadlineStats";
import SelectBox, { SelectItem } from "./SelectBox";
import Table from "./Table";
import { closePreview } from "./actions";

const FullScreen = styled("div")`
  height: 100%;
  width: 100%;
  position: fixed;
  top: 0;
  left: 0;
  background-color: ${({ theme }) => theme.col.bgAlt};
  padding: 60px 20px 20px;
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
  border-radius: ${({theme}) => theme.borderRadius};
`;

export default function Preview() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const getStats = usePreviewStatistics();
  const [query] = useState<number>(12);
  const [statistics, setStatistics] =
    useState<PreviewStatisticsResponse | null>(null);

  
  const [selectBoxOpen, setSelectBoxOpen] = useState<boolean>(false);
  const [page, setPage] = useState<number>(0);
  const [popOver, setPopOver] = useState<PreviewStatistics | null>(null);
  const onClose = () => dispatch(closePreview());
  
  useHotkeys("esc", () => {
    onClose();
  });

  useEffect(() => {
    async function fetchData() {
      try {
        setStatistics(await getStats(query));
      } catch (e) {
        dispatch(
          setMessage({
            message: t("preview.loadingError"),
            type: SnackMessageType.ERROR,
          }),
        );
        console.error(e);
      }
    }
    fetchData();
  }, [query, dispatch, getStats, t]);

  useEffect(() => {
    setPage(0);
  }, [statistics]);

  return (
    <FullScreen>
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
            const index = statistics?.statistics.findIndex(
              (stat) => stat.name === res.name,
            );
            if (index !== undefined && index !== null) {
              setPage(Math.floor(index / 4));
              setSelectBoxOpen(false);
            }
          }}
          isOpen={selectBoxOpen}
          setIsOpen={setSelectBoxOpen}
        />
        {statistics === null ? (
          <HeadlineStats loading={true} />
        ) : (
          <HeadlineStats
            numberOfRows={statistics.total}
            dateRange={statistics.dateRange}
            missingValues={statistics.statistics.reduce(
              (acc, obj) => acc + (obj.nullValues ?? 0),
              0,
            )}
            loading={false}
          />
        )}
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
      <Table />
    </FullScreen>
  );
}
