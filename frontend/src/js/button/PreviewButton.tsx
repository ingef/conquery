import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";

import { StateT } from "../app/reducers";
import { openPreview, useLoadPreviewData } from "../preview-v2/actions";
import { TransparentButton } from "./TransparentButton";

const Button = styled(TransparentButton)`
  white-space: nowrap;
  height: 35px;
`;

const PreviewButton = ({
  url,
  columns,
  ...restProps
}: {
  columns: ColumnDescription[];
  url: string;
}) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const loadPreviewData = useLoadPreviewData();
  const queryId = useSelector<StateT, string | null>(
    (state) => state.preview.lastQuery,
  );

  return (
    <Button
      onClick={async () => {
        if (queryId) {
          await loadPreviewData(queryId);
          dispatch(openPreview());
        }
      }}
      {...restProps}
    >
      {t("preview.preview")}
    </Button>
  );
};

export default PreviewButton;
