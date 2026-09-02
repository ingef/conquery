import { faEye, faEyeSlash } from "@fortawesome/free-regular-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

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
      <TooltipTrigger>
        <IconButton
          className="px-[10px] py-2"
          onClick={toggleBlurred}
          icon={blurred ? faEyeSlash : faEye}
        />
        <Tooltip>{t("history.blurred")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(VisibilityControl);
