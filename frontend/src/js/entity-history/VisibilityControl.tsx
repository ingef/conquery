import { EyeIcon, EyeOffIcon } from "lucide-react";
import { memo } from "react";
import { useTranslation } from "react-i18next";
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
          {blurred ? <EyeOffIcon /> : <EyeIcon />}
        </ToggleButton>
        <Tooltip placement="right">{t("history.blurred")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(VisibilityControl);
