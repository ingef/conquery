import styled from "@emotion/styled";
import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../tooltip/ConfirmableTooltip";
import WithTooltip from "../tooltip/WithTooltip";

const SxWithTooltip = styled(WithTooltip)`
  white-space: nowrap;
`;

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
      <SxWithTooltip text={text}>
        <IconButton icon={faTrash} active />
      </SxWithTooltip>
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
