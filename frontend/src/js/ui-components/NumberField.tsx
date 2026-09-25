import { ChevronDownIcon, ChevronUpIcon } from "lucide-react";
import { type Ref, useRef } from "react";
import {
  ButtonContext,
  NumberField as RacNumberField,
  type NumberFieldProps as RacNumberFieldProps,
} from "react-aria-components";
import { mergeRefs } from "react-merge-refs";

import { exists } from "../common/helpers/exists";
import { FieldError } from "./FieldError";
import { Input, InputButton, InputClearButton, InputText } from "./Input";
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

// react-aria labels the slots, disables them at a bound and repeats while held
const Stepper = () => (
  <span className="flex flex-col">
    <InputButton half slot="increment">
      <ChevronUpIcon />
    </InputButton>
    <InputButton half slot="decrement">
      <ChevronDownIcon />
    </InputButton>
  </span>
);

/**
 * A number field on react-aria's NumberField: parsed and formatted in the
 * app's locale, clamped and snapped to `step` on commit (blur, Enter, the
 * stepper), cleared with a button while it holds text.
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
}: NumberFieldProps) => {
  const ownInputRef = useRef<HTMLInputElement>(null);

  return (
    <RacNumberField
      className="min-w-0"
      validationBehavior="aria"
      isInvalid={exists(errorMessage)}
      isWheelDisabled
      value={value ?? Number.NaN}
      onChange={(next) => onChange(Number.isNaN(next) ? null : next)}
      {...props}
    >
      {({ isDisabled, isReadOnly, isInvalid, state }) => (
        <>
          {label && (
            <Label size={labelSize} indexPrefix={indexPrefix} tooltip={tooltip}>
              {label}
            </Label>
          )}
          <Input
            ref={mergeRefs([ownInputRef, inputRef])}
            placeholder={placeholder}
            isDisabled={isDisabled}
            isInvalid={isInvalid}
            addonRight={
              <>
                {unit && <InputText>{unit}</InputText>}
                {state.inputValue !== "" && (
                  // the number field hands stepper slots to every Button inside; this one clears
                  <ButtonContext.Provider value={null}>
                    <InputClearButton
                      isDisabled={isDisabled || isReadOnly}
                      onPress={() => {
                        state.setInputValue("");
                        state.setNumberValue(Number.NaN);
                        ownInputRef.current?.focus();
                      }}
                    />
                  </ButtonContext.Provider>
                )}
                <Stepper />
              </>
            }
          />
          <FieldError>{errorMessage}</FieldError>
        </>
      )}
    </RacNumberField>
  );
};
