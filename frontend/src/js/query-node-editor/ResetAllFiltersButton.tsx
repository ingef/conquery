import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const SxWithTooltip = styled(WithTooltip)`
  text-transform: uppercase;
  white-space: nowrap;
`;

interface Props {
  compact?: boolean;
  icon: "undo" | "trash";
  onClick: () => void;
}

const ResetAllFiltersButton: FC<Props> = ({ compact, icon, onClick }) => {
  const { t } = useTranslation();

  return (
    <SxWithTooltip
      text={compact ? t("queryNodeEditor.resetSettings") : undefined}
    >
      <IconButton active onClick={onClick} icon={icon}>
        {!compact && t("queryNodeEditor.resetSettings")}
      </IconButton>
    </SxWithTooltip>
  );
};

export default ResetAllFiltersButton;
