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
  options: SelectOptionT[];
  disabled?: boolean;
  small?: boolean;
  selectProps?: Object;
  tooltip?: string;
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
  options,
  disabled,
  selectProps,
  tooltip,
}: PropsT) => {
  const { t } = useTranslation();
  const selected = options && options.find((v) => v.value === input.value);
  const defaultValue =
    options && options.find((v) => v.value === input.defaultValue);

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
      <ReactSelect<false>
        highlightChanged
        name="form-field"
        small={small}
        value={selected}
        defaultValue={defaultValue}
        options={options}
        onChange={(field: { value: string; label: string } | null) =>
          field ? input.onChange(field.value) : input.onChange(null)
        }
        isSearchable={false}
        isClearable={input.clearable}
        isDisabled={!!disabled}
        placeholder={t("reactSelect.placeholder")}
        noOptionsMessage={() => t("reactSelect.noResults")}
        {...selectProps}
      />
    </Labeled>
  );
};

export default InputSelect;
