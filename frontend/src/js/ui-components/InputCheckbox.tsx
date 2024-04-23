import styled from "@emotion/styled";

import { faCheck } from "@fortawesome/free-solid-svg-icons";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import InfoTooltip from "../tooltip/InfoTooltip";
import WithTooltip from "../tooltip/WithTooltip";

const Row = styled("div")<{ $disabled?: boolean }>`
  display: flex;
  flex-direction: row;
  align-items: center;
  cursor: ${({ $disabled }) => ($disabled ? "not-allowed" : "pointer")};
`;

const Label = styled("span")`
  margin-left: 10px;
  font-size: ${({ theme }) => theme.font.sm};
  line-height: 1;
`;

const Container = styled("div")<{ $disabled?: boolean }>`
  flex-shrink: 0;
  position: relative;
  font-size: 22px;
  width: 20px;
  height: 20px;
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  border-radius: ${({ theme }) => theme.borderRadius};
  box-sizing: content-box;
  opacity: ${({ $disabled }) => ($disabled ? 0.5 : 1)};
`;

const InputCheckbox = ({
  label,
  className,
  tooltip,
  tooltipLazy,
  infoTooltip,
  value,
  onChange,
  disabled,
}: {
  label: string;
  className?: string;
  tooltip?: string;
  tooltipLazy?: boolean;
  infoTooltip?: string;
  value?: boolean;
  onChange: (checked: boolean) => void;
  disabled?: boolean;
}) => (
  <Row
    className={className}
    onClick={() => {
      if (!disabled) onChange(!value);
    }}
    $disabled={disabled}
  >
    <WithTooltip text={tooltip} lazy={tooltipLazy}>
      <Container $disabled={disabled}>
        {!!value && (
          <div className="absolute top-0 left-0 w-5 h-5 bg-primary-500 flex items-center justify-center text-white">
            <FaIcon icon={faCheck} className="!text-white scale-125" />
          </div>
        )}
      </Container>
    </WithTooltip>
    <Label>{label}</Label>
    {exists(infoTooltip) && <InfoTooltip text={infoTooltip} />}
  </Row>
);

export default InputCheckbox;
