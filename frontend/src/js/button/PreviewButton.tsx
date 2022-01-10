import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC, useContext } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";
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
  const { authToken } = useContext(AuthTokenContext);
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.preview.isLoading,
  );
  const { t } = useTranslation();

  const openPreview = useOpenPreview();
  const onOpenPreview = (url: string) => openPreview(url, columns);

  const href = `${url}?access_token=${encodeURIComponent(
    authToken,
  )}&charset=utf-8&pretty=false`;

  return (
    <WithTooltip text={t("preview.preview")} className={className}>
      <SxIconButton
        icon={isLoading ? "spinner" : "search"}
        onClick={() => onOpenPreview(href)}
        {...restProps}
      />
    </WithTooltip>
  );
};

export default PreviewButton;
