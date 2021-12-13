import styled from "@emotion/styled";
import { FC } from "react";

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
  text: string;
  variant?: "primary" | "secondary";
}

const ResetAllSettingsButton: FC<Props> = ({
  compact,
  icon,
  text,
  onClick,
  variant = "primary",
}) => {
  return (
    <SxWithTooltip text={compact ? text : undefined}>
      <IconButton
        onClick={onClick}
        icon={icon}
        active={variant === "primary"}
        secondary={variant === "secondary"}
      >
        {!compact && text}
      </IconButton>
    </SxWithTooltip>
  );
};

export default ResetAllSettingsButton;
