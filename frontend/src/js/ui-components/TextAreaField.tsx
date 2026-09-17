import type { Ref } from "react";
import {
  TextField as RacTextField,
  type TextFieldProps as RacTextFieldProps,
  TextArea,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { exists } from "../common/helpers/exists";
import { FieldError } from "./FieldError";
import { type FieldLabelProps, Label } from "./Label";

const textArea = tv({
  base: [
    "w-full",
    "rounded",
    "border border-gray-400",
    "bg-white",
    "py-[6px] px-[10px]",
    "text-sm font-normal text-gray-800",
    "outline-none",
    "placeholder:text-gray-400",
    "data-focus-visible:outline-2 data-focus-visible:outline-primary-500",
    "data-disabled:opacity-50 data-disabled:cursor-not-allowed",
    "data-invalid:border-red",
  ],
});

export type TextAreaFieldProps = Omit<
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
    placeholder?: string;
    rows?: number;
    indexPrefix?: number;
    /** a help icon after the label */
    tooltip?: string;
    /** shown below the text area; the field is invalid while it is set */
    errorMessage?: string;
    /** the text area element, e.g. to focus it */
    inputRef?: Ref<HTMLTextAreaElement>;
  };

/** A multi-line text field, react-aria's TextField with a TextArea. */
export const TextAreaField = ({
  label,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  rows,
  inputRef,
  ...props
}: TextAreaFieldProps) => (
  <RacTextField
    className="min-w-0"
    validationBehavior="aria"
    isInvalid={exists(errorMessage)}
    {...props}
  >
    {label && (
      <Label indexPrefix={indexPrefix} tooltip={tooltip}>
        {label}
      </Label>
    )}
    <TextArea
      ref={inputRef}
      className={textArea()}
      placeholder={placeholder}
      rows={rows}
    />
    <FieldError>{errorMessage}</FieldError>
  </RacTextField>
);
