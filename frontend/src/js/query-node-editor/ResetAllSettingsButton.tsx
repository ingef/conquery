import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../ui-components/ConfirmableTooltip";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

const ResetAllSettingsButton = ({
  compact,
  onClick,
}: {
  compact?: boolean;
  onClick: () => void;
}) => {
  const { t } = useTranslation();
  const text = t("queryNodeEditor.clearAllSettings");
  const confirmationText = t("queryNodeEditor.clearAllSettingsConfirm");

  const trigger = (
    <IconButton icon={faTrash} active>
      {compact ? null : text}
    </IconButton>
  );

  // tippy needs the button itself as its child, the tooltip goes around both
  return compact ? (
    <TooltipTrigger>
      <ConfirmableTooltip
        onConfirm={onClick}
        confirmationText={confirmationText}
      >
        {trigger}
      </ConfirmableTooltip>
      <Tooltip className="whitespace-nowrap">{text}</Tooltip>
    </TooltipTrigger>
  ) : (
    <ConfirmableTooltip onConfirm={onClick} confirmationText={confirmationText}>
      {trigger}
    </ConfirmableTooltip>
  );
};

export default ResetAllSettingsButton;
