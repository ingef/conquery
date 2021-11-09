import styled from "@emotion/styled";

import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";
import WithTooltip from "../tooltip/WithTooltip";

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: pointer;
`;

const Label = styled("span")`
  margin-left: 10px;
  font-size: ${({ theme }) => theme.font.sm};
  line-height: 1;
`;

const Container = styled("div")`
  flex-shrink: 0;
  position: relative;
  font-size: 22px;
  width: 20px;
  height: 20px;
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
  box-sizing: content-box;
`;

const Checkmark = styled("div")`
  position: absolute;
  top: 0;
  left: 0;
  height: 20px;
  width: 20px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};

  &:after {
    content: "";
    position: absolute;
    left: 6px;
    top: 2px;
    width: 5px;
    height: 10px;
    border: solid white;
    border-width: 0 3px 3px 0;
    transform: rotate(45deg);
  }
`;

interface PropsType {
  label: string;
  className?: string;
  tooltip?: string;
  tooltipLazy?: boolean;
  infoTooltip?: string;
  value?: boolean;
  onChange: (checked: boolean) => void;
}

const InputCheckbox = ({
  label,
  className,
  tooltip,
  tooltipLazy,
  infoTooltip,
  value,
  onChange,
}: PropsType) => (
  <Row className={className} onClick={() => onChange(!value)}>
    <WithTooltip text={tooltip} lazy={tooltipLazy}>
      <Container>{!!value && <Checkmark />}</Container>
    </WithTooltip>
    <Label>{label}</Label>
    {exists(infoTooltip) && <InfoTooltip text={infoTooltip} />}
  </Row>
);

export default InputCheckbox;
