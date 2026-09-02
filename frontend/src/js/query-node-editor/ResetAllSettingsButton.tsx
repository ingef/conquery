import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { ConfirmPopover } from "../ui-components/ConfirmPopover";
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

  return compact ? (
    <TooltipTrigger>
      <ConfirmPopover onConfirm={onClick} confirmationText={confirmationText}>
        {trigger}
      </ConfirmPopover>
      <Tooltip className="whitespace-nowrap">{text}</Tooltip>
    </TooltipTrigger>
  ) : (
    <ConfirmPopover onConfirm={onClick} confirmationText={confirmationText}>
      {trigger}
    </ConfirmPopover>
  );
};

export default ResetAllSettingsButton;
