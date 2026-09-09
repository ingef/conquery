import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { Button } from "../ui-components/Button";
import { ConfirmMenu } from "../ui-components/ConfirmMenu";
import { Icon } from "../ui-components/Icon";
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
      <Icon icon={faTrash} />
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
