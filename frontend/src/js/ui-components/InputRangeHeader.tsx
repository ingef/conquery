import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "./InfoTooltip";

import Label from "./Label";

const InputRangeHeader = ({
  label,
  indexPrefix,
  unit,
  className,
  tooltip,
  disabled,
}: {
  className?: string;
  label: string;
  indexPrefix?: number;
  unit?: string;
  tooltip?: string;
  disabled?: boolean;
}) => {
  return (
    <Label className={className} disabled={disabled}>
      {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
      {label}
      {unit && ` ( ${unit} )`}
      {tooltip && <InfoTooltip text={tooltip} />}
    </Label>
  );
};

export default InputRangeHeader;
