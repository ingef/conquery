import {
  faMagnifyingGlass,
  faSpinner,
} from "@fortawesome/free-solid-svg-icons";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import type { StateT } from "../app/reducers";
import { openPreview, useLoadPreviewData } from "../preview/actions";
import { tv } from "../tv";
import IconButton, { type IconButtonPropsT } from "./IconButton";

const previewButton = tv({
  base: ["whitespace-nowrap", "h-[35px]", "px-3 py-[5px]"],
});

const PreviewButton = ({
  className,
  ...buttonProps
}: Partial<IconButtonPropsT>) => {
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
    <IconButton
      className={previewButton({ className })}
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
      {...buttonProps}
    >
      {t("preview.preview")}
    </IconButton>
  );
};

export default PreviewButton;
