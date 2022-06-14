import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openPreview, useLoadPreviewData } from "../preview/actions";
import WithTooltip from "../tooltip/WithTooltip";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
  padding: 5px 6px;
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
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.preview.isLoading,
  );

  const loadPreviewData = useLoadPreviewData();
  const getAuthorizedUrl = useGetAuthorizedUrl();

  return (
    <WithTooltip text={t("preview.preview")} className={className}>
      <SxIconButton
        icon={isLoading ? "spinner" : "search"}
        onClick={async () => {
          await loadPreviewData(getAuthorizedUrl(url), columns);
          dispatch(openPreview());
        }}
        {...restProps}
      />
    </WithTooltip>
  );
};

export default PreviewButton;
