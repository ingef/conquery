import type { Ref } from "react";
import {
  TextField as RacTextField,
  type TextFieldProps as RacTextFieldProps,
} from "react-aria-components";

import { exists } from "../common/helpers/exists";
import { FieldError } from "./FieldError";
import { Input } from "./Input";
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

/** A single-line text field on react-aria's TextField; `value` / `onChange` carry a string. */
export const TextField = ({
  label,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  inputRef,
  ...props
}: TextFieldProps) => (
  <RacTextField
    className="min-w-0"
    validationBehavior="aria"
    isInvalid={exists(errorMessage)}
    {...props}
  >
    {({ isDisabled, isInvalid }) => (
      <>
        {label && (
          <Label indexPrefix={indexPrefix} tooltip={tooltip}>
            {label}
          </Label>
        )}
        <Input
          ref={inputRef}
          placeholder={placeholder}
          isDisabled={isDisabled}
          isInvalid={isInvalid}
        />
        <FieldError>{errorMessage}</FieldError>
      </>
    )}
  </RacTextField>
);
