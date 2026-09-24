import type { Ref } from "react";
import {
  NumberField as RacNumberField,
  type NumberFieldProps as RacNumberFieldProps,
} from "react-aria-components";

import { exists } from "../common/helpers/exists";
import { FieldError } from "./FieldError";
import { Input, InputText } from "./Input";
import { type FieldLabelProps, Label } from "./Label";

export type NumberFieldProps = Omit<
  RacNumberFieldProps,
  | "className"
  | "style"
  | "children"
  | "isInvalid"
  | "validate"
  | "validationBehavior"
  | "value"
  | "defaultValue"
  | "onChange"
  | "isWheelDisabled"
  | "aria-label"
> &
  FieldLabelProps & {
    /** null while empty */
    value: number | null;
    onChange: (value: number | null) => void;
    placeholder?: string;
    /** shown after the number, not part of the value */
    unit?: string;
    indexPrefix?: number;
    /** a help icon after the label */
    tooltip?: string;
    /** `sm` for a sub-label inside a labeled group */
    labelSize?: "sm" | "md";
    /** shown below the input; the field is invalid while it is set */
    errorMessage?: string;
    /** the input element, e.g. to focus it */
    inputRef?: Ref<HTMLInputElement>;
  };

/**
 * A number field on react-aria's NumberField: parsed and formatted in the
 * app's locale, clamped and snapped to `step` on commit (blur, Enter).
 */
export const NumberField = ({
  label,
  labelSize,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  unit,
  value,
  onChange,
  inputRef,
  ...props
}: NumberFieldProps) => (
  <RacNumberField
    className="min-w-0"
    validationBehavior="aria"
    isInvalid={exists(errorMessage)}
    isWheelDisabled
    value={value ?? Number.NaN}
    onChange={(next) => onChange(Number.isNaN(next) ? null : next)}
    {...props}
  >
    {({ isDisabled, isInvalid }) => (
      <>
        {label && (
          <Label size={labelSize} indexPrefix={indexPrefix} tooltip={tooltip}>
            {label}
          </Label>
        )}
        <Input
          ref={inputRef}
          placeholder={placeholder}
          isDisabled={isDisabled}
          isInvalid={isInvalid}
          addonRight={unit && <InputText>{unit}</InputText>}
        />
        <FieldError>{errorMessage}</FieldError>
      </>
    )}
  </RacNumberField>
);
