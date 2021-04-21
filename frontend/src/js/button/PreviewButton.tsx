import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import type { ColumnDescription } from "../api/types";
import { useAuthToken } from "../api/useApi";
import { openPreview } from "../preview/actions";

import IconButton from "./IconButton";

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
  const authToken = useAuthToken();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.preview.isLoading,
  );
  const { t } = useTranslation();

  const dispatch = useDispatch();
  const onOpenPreview = (url: string) => dispatch(openPreview(url, columns));

  const href = `${url}?access_token=${encodeURIComponent(
    authToken,
  )}&charset=utf-8&pretty=false`;

  return (
    <IconButton
      icon={isLoading ? "spinner" : "search"}
      onClick={() => onOpenPreview(href)}
      {...restProps}
    >
      {t("preview.preview")}
    </IconButton>
  );
};

export default PreviewButton;
