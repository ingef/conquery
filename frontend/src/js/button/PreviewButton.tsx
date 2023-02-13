import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import type { ColumnDescription, ResultUrlsWithLabel } from "../api/types";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openPreview, useLoadPreviewData } from "../preview/actions";

import { TransparentButton } from "./TransparentButton";

const Button = styled(TransparentButton)`
  white-space: nowrap;
  height: 35px;
`;

interface PropsT {
  columns: ColumnDescription[];
  url: ResultUrlsWithLabel;
}

const PreviewButton: FC<PropsT> = ({ url, columns, ...restProps }) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const loadPreviewData = useLoadPreviewData();
  const getAuthorizedUrl = useGetAuthorizedUrl();

  return (
    <Button
      onClick={async () => {
        await loadPreviewData(
          { ...url, url: getAuthorizedUrl(url.url) },
          columns,
        );
        dispatch(openPreview());
      }}
      {...restProps}
    >
      {t("preview.preview")}
    </Button>
  );
};

export default PreviewButton;
