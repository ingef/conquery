import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useMemo } from "react";
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

  const button = useMemo(() => {
    return compact ? (
      <TooltipTrigger>
        <IconButton icon={faTrash} active />
        <Tooltip className="whitespace-nowrap">{text}</Tooltip>
      </TooltipTrigger>
    ) : (
      <IconButton icon={faTrash} active>
        {text}
      </IconButton>
    );
  }, [compact, text]);

  return (
    <ConfirmableTooltip onConfirm={onClick} confirmationText={confirmationText}>
      {button}
    </ConfirmableTooltip>
  );
};

export default ResetAllSettingsButton;
