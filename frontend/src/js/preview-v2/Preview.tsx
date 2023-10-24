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
import { useEffect, useState } from "react";
import { Icon } from "../icon/FaIcon";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { Table as arrowTable, tableFromArrays } from 'apache-arrow';

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

async function load() {

  const LENGTH = 500;

  const rainAmounts = Float32Array.from(
    { length: LENGTH },
    () => Number((Math.random() * 20).toFixed(1)));

  const rainDates = Array.from(
    { length: LENGTH },
    (_, i) => new Date(Date.now() - 1000 * 60 * 60 * 24 * i));
  
  console.log(rainAmounts);
  console.log(rainDates);
  const table = tableFromArrays({
    precipitation: rainAmounts,
    date: rainDates
  });
  return table;
}

export default function Preview() {
  const dispatch = useDispatch();
  const { t } = useTranslation();

  const onClose = () => dispatch(closePreview());

  useHotkeys("esc", () => {
    onClose();
  });
  let [loaded, setLoaded] = useState(false);
  let [data, setData] = useState<arrowTable|null>(null);

  useEffect(() => {
    if (loaded) return;
    load().then((data) => {
      setLoaded(true);
      setData(data);
    });
  }, [loaded])

  return (
    <FullScreen>
      {loaded && data ? (
        <>
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
          <Charts />
          <Table
            data={data}
            columns={[]}
          />
        </>
      ) : (
          <><Icon icon={faSpinner}/></>
      )}
    </FullScreen>
  );
}
