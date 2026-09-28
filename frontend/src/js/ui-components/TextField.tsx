import { type Ref, useRef } from "react";
import {
  TextField as RacTextField,
  type TextFieldProps as RacTextFieldProps,
} from "react-aria-components";
import { mergeRefs } from "react-merge-refs";

import { exists } from "../common/helpers/exists";
import { FieldError } from "./FieldError";
import { Input, InputClearButton } from "./Input";
import { type FieldLabelProps, Label } from "./Label";

export type TextFieldProps = Omit<
  RacTextFieldProps,
  | "className"
  | "style"
  | "children"
  | "isInvalid"
  | "validate"
  | "validationBehavior"
  | "type"
  | "aria-label"
> &
  FieldLabelProps & {
    type?: "text" | "search" | "url" | "tel" | "email" | "password";
    placeholder?: string;
    indexPrefix?: number;
    /** a help icon after the label */
    tooltip?: string;
    /** shown below the input; the field is invalid while it is set */
    errorMessage?: string;
    /** the input element, e.g. to focus it */
    inputRef?: Ref<HTMLInputElement>;
  };

/**
 * A single-line text field on react-aria's TextField; `value` / `onChange`
 * carry a string, and a button clears it while there is one.
 */
export const TextField = ({
  label,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  inputRef,
  value,
  onChange,
  ...props
}: TextFieldProps) => {
  const ownInputRef = useRef<HTMLInputElement>(null);

  return (
    <RacTextField
      className="min-w-0"
      validationBehavior="aria"
      isInvalid={exists(errorMessage)}
      value={value}
      onChange={onChange}
      {...props}
    >
      {({ isDisabled, isReadOnly, isInvalid }) => (
        <>
          {label && (
            <Label indexPrefix={indexPrefix} tooltip={tooltip}>
              {label}
            </Label>
          )}
          <Input
            ref={mergeRefs([ownInputRef, inputRef])}
            placeholder={placeholder}
            isDisabled={isDisabled}
            isInvalid={isInvalid}
            addonRight={
              value && (
                <InputClearButton
                  isDisabled={isDisabled || isReadOnly}
                  onPress={() => {
                    onChange?.("");
                    ownInputRef.current?.focus();
                  }}
                />
              )
            }
          />
          <FieldError>{errorMessage}</FieldError>
        </>
      )}
    </RacTextField>
  );
};
