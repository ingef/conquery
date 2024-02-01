import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";

import {
  faMagnifyingGlass,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useMemo, useState } from "react";
import { StateT } from "../app/reducers";
import { openPreview, useLoadPreviewData } from "../preview-v2/actions";
import IconButton from "./IconButton";

const Button = styled(IconButton)`
  white-space: nowrap;
  height: 35px;
  padding: 5px 12px;
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

  const [isLoading, setLoading] = useState(false);
  const icon = useMemo(
    () => (isLoading ? faSpinner : faMagnifyingGlass),
    [isLoading],
  );

  return (
    <Button
      frame
      icon={icon}
      onClick={async () => {
        if (queryId) {
          setLoading(true);
          setTimeout(async () => {
            await loadPreviewData(queryId);
            setLoading(false);
            dispatch(openPreview());
          });
        }
      }}
      {...restProps}
    >
      {t("preview.preview")}
    </Button>
  );
};

export default PreviewButton;
