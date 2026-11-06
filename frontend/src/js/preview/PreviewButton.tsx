import { LoaderCircleIcon, SearchIcon } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { Button, type ButtonProps } from "../ui-components/Button";
import { openPreview, useLoadPreviewData } from "./actions";

const PreviewButton = (props: ButtonProps) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const loadPreviewData = useLoadPreviewData();
  const queryId = useSelector<StateT, string | null>(
    (state) => state.preview.lastQuery,
  );

  const [isLoading, setLoading] = useState(false);

  return (
    <Button
      intent="secondary"
      onPress={async () => {
        if (queryId) {
          setLoading(true);
          setTimeout(async () => {
            await loadPreviewData(queryId);
            setLoading(false);
            dispatch(openPreview());
          });
        }
      }}
      {...props}
    >
      {isLoading ? <LoaderCircleIcon /> : <SearchIcon />}
      {t("preview.preview")}
    </Button>
  );
};

export default PreviewButton;
