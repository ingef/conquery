import { faEye, faEyeSlash } from "@fortawesome/free-regular-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { Icon } from "../ui-components/Icon";
import { ToggleButton } from "../ui-components/ToggleButton";
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
        <ToggleButton
          aria-label={t("history.blurred")}
          isSelected={!!blurred}
          onChange={toggleBlurred}
        >
          <Icon icon={blurred ? faEyeSlash : faEye} />
        </ToggleButton>
        <Tooltip placement="right">{t("history.blurred")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(VisibilityControl);
