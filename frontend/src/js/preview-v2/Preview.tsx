import styled from "@emotion/styled";
import { useEffect, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { StateT } from "../app/reducers";
import { TransparentButton } from "../button/TransparentButton";
import PreviewInfo from "../preview/PreviewInfo";
import { useDispatch } from "react-redux";

import { usePreviewStatistics } from "../api/api";
import { PreviewStatistics, PreviewStatisticsResponse } from "../api/types";
import { TransparentButton } from "../button/TransparentButton";
import PreviewInfo from "../preview/PreviewInfo";
import { setMessage } from "../snack-message/actions";
import { SnackMessageType } from "../snack-message/reducer";

import Charts from "./Charts";
import HeadlineStats from "./HeadlineStats";
import Table from "./Table";
import { closePreview } from "./actions";
import { PreviewStateT } from "./reducer";
import DiagramModal from "./DiagramModal";
import FaIcon from "../icon/FaIcon";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { stat } from "fs";

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

const ScrollBox = styled("div")`
  overflow: auto;
  padding: 60px 20px 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
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

export default function Preview() {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const getStats = usePreviewStatistics();
  const [query, _] = useState<number>(12);
  const [statistics, setStatistics] =
    useState<PreviewStatisticsResponse | null>(null);
  
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
  }, [query]);

  return (
    <FullScreen>
      <ScrollBox>
        <PreviewInfo
          rawPreviewData={[]}
          columns={[]}
          onClose={onClose}
          minDate={new Date()}
          maxDate={new Date()}
        />
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

        {
          statistics === null ? (
            <HeadlineStats
              loading={true}
            />
          ) : (
              <HeadlineStats
                numberOfRows={statistics.total}
                dateRange={statistics.dateRange}
                missingValues={statistics.statistics.reduce((acc, obj) => acc + (obj.nullValues ?? 0), 0)}
                loading={false}
              />
          )
        }
        SelectBox (Konzept Liste)
      {statistics ? (
        <SxCharts
          statistics={statistics.statistics}
          showPopup={(statistic: PreviewStatistics) => {
            console.log(statistic);
            setPopOver(statistic);
          }}
        /> 
      ) : (
        <SxChartLoadingBlocker>
            <SxFaIcon icon={faSpinner} />
        </SxChartLoadingBlocker>
      )
      }
      {popOver && (
        <DiagramModal
          statistic={popOver}
          onClose={() => setPopOver(null)}
        />
      )}
        {preview.tableData && <Table data={preview.tableData} />}
      </ScrollBox>
    </FullScreen>
  );
}
