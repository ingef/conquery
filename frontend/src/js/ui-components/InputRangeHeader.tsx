import { FC } from "react";

import { IndexPrefix } from "../common/components/IndexPrefix";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";

import Label from "./Label";

interface PropsT {
  className?: string;
  label: string;
  indexPrefix?: number;
  unit?: string;
  tooltip?: string;
  disabled?: boolean;
}

const InputRangeHeader: FC<PropsT> = ({
  label,
  indexPrefix,
  unit,
  className,
  tooltip,
  disabled,
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
