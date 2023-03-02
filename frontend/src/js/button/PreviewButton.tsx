import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import type { ColumnDescription } from "../api/types";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openPreview, useLoadPreviewData } from "../preview/actions";

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
  const getAuthorizedUrl = useGetAuthorizedUrl();

  return (
    <Button
      onClick={async () => {
        await loadPreviewData(getAuthorizedUrl(url), columns);
        dispatch(openPreview());
      }}
      {...restProps}
    >
      {t("preview.preview")}
    </Button>
  );
};

export default PreviewButton;
