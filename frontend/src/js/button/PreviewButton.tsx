import {
  faMagnifyingGlass,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { openPreview, useLoadPreviewData } from "../preview/actions";
import { Button, type ButtonProps } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";

const PreviewButton = (props: ButtonProps) => {
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
      <Icon icon={icon} />
      {t("preview.preview")}
    </Button>
  );
};

export default PreviewButton;
