import { memo } from "react";
import { useTranslation } from "react-i18next";
import type { NodeResetConfig } from "../model/node";
import { Button } from "../ui-components/Button";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import ResetAllSettingsButton from "./ResetAllSettingsButton";

interface Props {
  isCompact: boolean;
  showClearReset: boolean;
  onClose: () => void;
  onResetAllSettings: (config: NodeResetConfig) => void;
}

const ResetAndClose = ({
  showClearReset,
  isCompact,
  onClose,
  onResetAllSettings,
}: Props) => {
  const { t } = useTranslation();

  return (
    <div className="flex items-center">
      {showClearReset && (
        <ResetAllSettingsButton
          onClick={() => onResetAllSettings({ useDefaults: false })}
          compact={isCompact}
        />
      )}
      <TooltipTrigger>
        <Button intent="secondary" onPress={onClose}>
          {t("common.save")}
        </Button>
        <Tooltip>{t("common.saveAndCloseEsc")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default memo(ResetAndClose);
