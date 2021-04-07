import React, { FC } from "react";
import styled from "@emotion/styled";
import InfoTooltip from "../tooltip/InfoTooltip";

interface PropsT {
  className?: string;
  label: string;
  unit?: string;
  tooltip?: string;
  disabled?: boolean;
}

const Container = styled("p")<{ disabled?: boolean }>`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 6px 0 3px;
  color: ${({ theme, disabled }) => (disabled ? theme.col.gray : "initial")};
`;

const InputRangeHeader: FC<PropsT> = ({
  label,
  unit,
  className,
  tooltip,
  disabled,
}) => {
  return (
    <Container className={className} disabled={disabled}>
      {label}
      {unit && ` ( ${unit} )`}
      {tooltip && <InfoTooltip text={tooltip} />}
    </Container>
  );
};

export default InputRangeHeader;
