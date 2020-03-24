import React from "react";
import styled from "@emotion/styled";
import InfoTooltip from "../tooltip/InfoTooltip";

type PropsType = {
  className?: string;
  label: string;
  unit?: string;
  tooltip?: string;
  disabled?: boolean;
};

const InputRangeHeader = styled("p")`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 2px 8px;
  color: ${({ theme, disabled }) => (disabled ? theme.col.gray : "initial")};
`;

export default ({ label, unit, className, tooltip, disabled }: PropsType) => {
  return (
    <InputRangeHeader className={className} disabled={disabled}>
      {label}
      {unit && ` ( ${unit} )`}
      {tooltip && <InfoTooltip text={tooltip} />}
    </InputRangeHeader>
  );
};
