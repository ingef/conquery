import { faEye, faEyeSlash } from "@fortawesome/free-regular-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const VisibilityControl = ({
  blurred,
  toggleBlurred,
}: {
  blurred?: boolean;
  toggleBlurred: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col items-center">
      <WithTooltip text={t("history.blurred")}>
        <IconButton
          className="px-[10px] py-2"
          onClick={toggleBlurred}
          icon={blurred ? faEyeSlash : faEye}
        />
      </WithTooltip>
    </div>
  );
};

export default memo(VisibilityControl);
