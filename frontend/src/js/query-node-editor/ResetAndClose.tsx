import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";

import { TransparentButton } from "../button/TransparentButton";
import { NodeResetConfig } from "../model/node";
import WithTooltip from "../tooltip/WithTooltip";

import ResetAllSettingsButton from "./ResetAllSettingsButton";

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

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
    <Row>
      {showClearReset && (
        <ResetAllSettingsButton
          text={t("queryNodeEditor.clearAllSettings")}
          icon="trash"
          onClick={() => onResetAllSettings({ useDefaults: false })}
          compact={isCompact}
        />
      )}
      <WithTooltip text={t("common.saveAndCloseEsc")}>
        <TransparentButton small onClick={onClose}>
          {t("common.save")}
        </TransparentButton>
      </WithTooltip>
    </Row>
  );
};

export default memo(ResetAndClose);
