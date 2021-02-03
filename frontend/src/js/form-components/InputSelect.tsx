import React from "react";
import T from "i18n-react";

import ReactSelect from "./ReactSelect";
import Labeled from "./Labeled";

import { isEmpty } from "../common/helpers";
import type { SelectOptionT } from "../api/types";
import InfoTooltip from "../tooltip/InfoTooltip";

interface PropsT {
  className?: string;
  label?: string;
  options: SelectOptionT[];
  disabled?: boolean;
  small?: boolean;
  selectProps?: Object;
  tooltip?: string;
  input: {
    clearable?: boolean;
    defaultValue: string | null;
    value: string | null;
    onChange: (value: string | null) => void;
  };
}

const InputSelect = ({
  className,
  small,
  input,
  label,
  options,
  disabled,
  selectProps,
  tooltip,
}: PropsT) => {
  const selected = options && options.filter((v) => v.value === input.value);
  const defaultValue =
    options && options.filter((v) => v.value === input.defaultValue);

  return (
    <Labeled
      className={className}
      disabled={disabled}
      valueChanged={!isEmpty(input.value) && input.value !== input.defaultValue}
      label={
        <>
          {!!label && label}
          {!!tooltip && <InfoTooltip text={tooltip} />}
        </>
      }
    >
      <ReactSelect
        highlightChanged
        name="form-field"
        small={small}
        value={selected}
        defaultValue={defaultValue}
        options={options}
        onChange={(field: { value: string; label: string }) =>
          field ? input.onChange(field.value) : input.onChange(null)
        }
        isSearchable={false}
        isClearable={input.clearable}
        isDisabled={!!disabled}
        placeholder={T.translate("reactSelect.placeholder")}
        noOptionsMessage={() => T.translate("reactSelect.noResults")}
        {...selectProps}
      />
    </Labeled>
  );
};

export default InputSelect;
