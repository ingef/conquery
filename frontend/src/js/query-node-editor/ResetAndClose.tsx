import { memo } from "react";
import { useTranslation } from "react-i18next";

import { TransparentButton } from "../button/TransparentButton";
import type { NodeResetConfig } from "../model/node";
import WithTooltip from "../ui-components/WithTooltip";

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
      <WithTooltip text={t("common.saveAndCloseEsc")}>
        <TransparentButton small onClick={onClose}>
          {t("common.save")}
        </TransparentButton>
      </WithTooltip>
    </div>
  );
};

export default memo(ResetAndClose);
