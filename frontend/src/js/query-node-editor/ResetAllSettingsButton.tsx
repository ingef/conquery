import styled from "@emotion/styled";
import { FC } from "react";

import IconButton from "../button/IconButton";
import WithTooltip from "../tooltip/WithTooltip";

const SxWithTooltip = styled(WithTooltip)`
  white-space: nowrap;
`;

interface Props {
  compact?: boolean;
  icon: "undo" | "trash";
  onClick: () => void;
  text: string;
}

const ResetAllSettingsButton: FC<Props> = ({
  compact,
  icon,
  text,
  onClick,
}) => {
  return (
    <SxWithTooltip text={compact ? text : undefined}>
      <IconButton onClick={onClick} icon={icon} active>
        {!compact && text}
      </IconButton>
    </SxWithTooltip>
  );
};

export default ResetAllSettingsButton;
