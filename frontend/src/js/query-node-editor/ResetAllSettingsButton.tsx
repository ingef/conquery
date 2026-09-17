import { TrashIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
import { ConfirmMenu } from "../ui-components/ConfirmMenu";
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
    <Button intent="tertiary">
      <TrashIcon />
      {compact ? null : text}
    </Button>
  );

  return compact ? (
    <TooltipTrigger>
      <ConfirmMenu onConfirm={onClick} confirmationText={confirmationText}>
        {trigger}
      </ConfirmMenu>
      <Tooltip className="whitespace-nowrap">{text}</Tooltip>
    </TooltipTrigger>
  ) : (
    <ConfirmMenu onConfirm={onClick} confirmationText={confirmationText}>
      {trigger}
    </ConfirmMenu>
  );
};

export default ResetAllSettingsButton;
