import styled from "@emotion/styled";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { StateT } from "../app/reducers";
import { TransparentButton } from "../button/TransparentButton";
import PreviewInfo from "../preview/PreviewInfo";

import Charts from "./Charts";
import HeadlineStats from "./HeadlineStats";
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
  gap: 30px;
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
`;

export default function Preview() {
  const preview = useSelector<StateT, PreviewStateT>((state) => state.preview);
  const dispatch = useDispatch();
  const { t } = useTranslation();

  const onClose = () => dispatch(closePreview());

  useHotkeys("esc", () => {
    onClose();
  });

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
        <Headline>
          <TransparentButton small onClick={onClose}>
            {t("common.back")}
          </TransparentButton>
          Ergebnisvorschau
          <HeadlineStats />
        </Headline>
        SelectBox (Konzept Liste)
        <Charts />
        {preview.tableData && <Table data={preview.tableData} />}
      </ScrollBox>
    </FullScreen>
  );
}
