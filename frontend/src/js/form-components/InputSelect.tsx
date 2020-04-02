import React from "react";
import T from "i18n-react";
import type { FieldPropsType } from "redux-form";

import ReactSelect from "./ReactSelect";
import Labeled from "./Labeled";

import { isEmpty } from "../common/helpers";
import type { SelectOptionsT } from "../api/types";
import InfoTooltip from "../tooltip/InfoTooltip";

type PropsType = FieldPropsType & {
  className?: string;
  label?: string;
  options: SelectOptionsT;
  disabled?: boolean;
  small?: boolean;
  selectProps?: Object;
  tooltip?: string;
};

const InputSelect = ({
  className,
  small,
  input,
  label,
  options,
  disabled,
  selectProps,
  tooltip
}: PropsType) => {
  const selected = options && options.filter(v => v.value === input.value);
  const defaultValue =
    options && options.filter(v => v.value === input.defaultValue);

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
        onChange={field =>
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
