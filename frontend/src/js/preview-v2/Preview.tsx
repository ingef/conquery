import styled from "@emotion/styled";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { TransparentButton } from "../button/TransparentButton";
import PreviewInfo from "../preview/PreviewInfo";
import { closePreview } from "./actions";
import Charts from "./Charts";
import HeadlineStats from "./HeadlineStats";
import Table from "./Table";
import { usePreviewStatistics } from "../api/api";
import { setMessage } from "../snack-message/actions";
import { SnackMessageType } from "../snack-message/reducer";
import { useEffect, useMemo, useState } from "react";
import { PreviewStatisticsResponse } from "../api/types";

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
  gap: 30px;
`;

const Headline = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 30px;
`;

export default function Preview() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const getStats = usePreviewStatistics();
  const [query, _] = useState<number>(12);
  const [statistics, setStatistics] = useState<PreviewStatisticsResponse | null>(null);
  const onClose = () => dispatch(closePreview());

  useHotkeys("esc", () => {
    onClose();
  });

  useEffect(() => {
    async function fetchData() {
      try {
        setStatistics(await getStats(query));
      } catch (e) {
        dispatch(setMessage({
          message: "Fehler beim laden der Vorschau",
          type: SnackMessageType.ERROR
        })); // TODO translate
        console.error(e);
      }
    }
    fetchData();
  }, [query])

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
        <HeadlineStats />
      </Headline>
      SelectBox (Konzept Liste)
      {
        statistics && <Charts statistics={statistics.statistics} />
      }
      <Table />
    </FullScreen>
  );
}
