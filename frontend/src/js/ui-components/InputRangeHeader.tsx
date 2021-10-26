import styled from "@emotion/styled";
import { FC } from "react";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";

interface PropsT {
  className?: string;
  label: string;
  indexPrefix?: number;
  unit?: string;
  tooltip?: string;
  disabled?: boolean;
}

const Container = styled("p")<{ disabled?: boolean }>`
  font-size: ${({ theme }) => theme.font.sm};
  margin: 6px 0 3px;
  color: ${({ theme, disabled }) => (disabled ? theme.col.gray : "initial")};
  display: flex;
  align-items: center;
`;

const InputRangeHeader: FC<PropsT> = ({
  label,
  indexPrefix,
  unit,
  className,
  tooltip,
  disabled,
}) => {
  return (
    <Container className={className} disabled={disabled}>
      {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
      {label}
      {unit && ` ( ${unit} )`}
      {tooltip && <InfoTooltip text={tooltip} />}
    </Container>
  );
};

export default InputRangeHeader;
