import styled from "@emotion/styled";
import { FC, useContext } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import { useOpenPreview } from "../preview/actions";
import WithTooltip from "../tooltip/WithTooltip";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

interface PropsT {
  columns: ColumnDescription[];
  url: string;
  className?: string;
}

const PreviewButton: FC<PropsT> = ({
  url,
  columns,
  className,
  ...restProps
}) => {
  const { t } = useTranslation();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.preview.isLoading,
  );

  const openPreview = useOpenPreview();
  const previewUrl = useAuthorizedPreviewUrl(url);

  return (
    <WithTooltip text={t("preview.preview")} className={className}>
      <SxIconButton
        icon={isLoading ? "spinner" : "search"}
        onClick={() => openPreview(previewUrl, columns)}
        {...restProps}
      />
    </WithTooltip>
  );
};
const useAuthorizedPreviewUrl = (url: string) => {
  const { authToken } = useContext(AuthTokenContext);

  const encodedAuthToken = encodeURIComponent(authToken);

  return `${url}?access_token=${encodedAuthToken}&charset=utf-8&pretty=false`;
};

export default PreviewButton;
