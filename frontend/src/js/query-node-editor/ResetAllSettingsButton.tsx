import styled from "@emotion/styled";
import { FC, useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../tooltip/ConfirmableTooltip";
import WithTooltip from "../tooltip/WithTooltip";

const SxWithTooltip = styled(WithTooltip)`
  white-space: nowrap;
`;

interface Props {
  compact?: boolean;
  onClick: () => void;
}

const ResetAllSettingsButton: FC<Props> = ({ compact, onClick }) => {
  const { t } = useTranslation();
  const text = t("queryNodeEditor.clearAllSettings");
  const confirmationText = t("queryNodeEditor.clearAllSettingsConfirm");

  const button = useMemo(() => {
    return compact ? (
      <SxWithTooltip text={text}>
        <IconButton icon="trash" active />
      </SxWithTooltip>
    ) : (
      <IconButton icon="trash" active>
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
