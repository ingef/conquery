import React from "react";
import { useTranslation } from "react-i18next";

import type { SelectOptionT } from "../api/types";
import { isEmpty } from "../common/helpers";
import InfoTooltip from "../tooltip/InfoTooltip";

import Labeled from "./Labeled";
import ReactSelect from "./ReactSelect";

interface PropsT {
  className?: string;
  label?: string;
  indexPrefix?: number;
  options: SelectOptionT[];
  disabled?: boolean;
  small?: boolean;
  selectProps?: Object;
  tooltip?: string;
  defaultValue?: string | null; // Weird to have it here as well => TODO: get rid of redux-form
  optional?: boolean;
  input: {
    clearable?: boolean;
    defaultValue?: string | null;
    value?: string | null;
    onChange: (value: string | null) => void;
  };
}

const InputSelect = ({
  className,
  small,
  input,
  label,
  indexPrefix,
  options,
  disabled,
  selectProps,
  tooltip,
  defaultValue,
  optional,
  ...rest
}: PropsT) => {
  const { t } = useTranslation();
  const selected = options && options.find((v) => v.value === input.value);
  const defaultVal =
    options &&
    options.find(
      (v) => v.value === defaultValue || v.value === input.defaultValue,
    );

  return (
    <Labeled
      className={className}
      disabled={disabled}
      valueChanged={!isEmpty(input.value) && input.value !== input.defaultValue}
      indexPrefix={indexPrefix}
      optional={optional}
      label={
        <>
          {!!label && label}
          {!!tooltip && <InfoTooltip text={tooltip} />}
        </>
      }
    >
      <ReactSelect<false>
        highlightChanged
        name="form-field"
        small={small}
        value={selected}
        defaultValue={defaultVal}
        options={options}
        isOptionDisabled={(option) => !!option.disabled}
        onChange={(field: { value: string; label: string } | null) =>
          field ? input.onChange(field.value) : input.onChange(null)
        }
        isSearchable={false}
        isClearable={input.clearable}
        isDisabled={!!disabled}
        placeholder={t("inputSelect.placeholder")}
        noOptionsMessage={() => t("inputSelect.empty")}
        {...selectProps}
      />
    </Labeled>
  );
};

export default InputSelect;
