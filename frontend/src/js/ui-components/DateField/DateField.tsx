import { CalendarIcon } from "lucide-react";
import { type Ref, useImperativeHandle, useRef, useState } from "react";
import {
  Dialog,
  TextField as RacTextField,
  type TextFieldProps as RacTextFieldProps,
} from "react-aria-components";
import ReactDatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { useTranslation } from "react-i18next";

import { formatDate, parseDate } from "../../common/helpers/dateHelper";
import { exists } from "../../common/helpers/exists";
import { FieldError } from "../FieldError";
import { Input, InputButton, InputClearButton } from "../Input";
import { type FieldLabelProps, Label } from "../Label";
import { Popover } from "../Popover";
import { CustomHeader } from "./CustomHeader";

export interface DateFieldHandle {
  openCalendar: () => void;
}

export type DateFieldProps = Omit<
  RacTextFieldProps,
  | "className"
  | "style"
  | "children"
  | "isInvalid"
  | "validate"
  | "validationBehavior"
  | "type"
  | "value"
  | "onChange"
  | "aria-label"
> &
  FieldLabelProps & {
    /** the text as typed, in `dateFormat` */
    value: string;
    onChange: (value: string) => void;
    dateFormat: string;
    placeholder?: string;
    indexPrefix?: number;
    /** a help icon after the label */
    tooltip?: string;
    /** shown below the input; the field is invalid while it is set */
    errorMessage?: string;
    /** a date was picked in the calendar */
    onCalendarSelect?: (value: string) => void;
    /** opens the calendar from outside */
    ref?: Ref<DateFieldHandle>;
  };

/**
 * A date typed as text, with a calendar to pick it from and a button that
 * clears it. The text is the value, so the caller can expand shortcuts
 * like `q2.2020`.
 */
export const DateField = ({
  ref,
  label,
  indexPrefix,
  tooltip,
  errorMessage,
  placeholder,
  value,
  dateFormat,
  onChange,
  onCalendarSelect,
  ...props
}: DateFieldProps) => {
  const { t } = useTranslation();
  const [isCalendarOpen, setCalendarOpen] = useState(false);
  const anchorRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useImperativeHandle(ref, () => ({
    openCalendar: () => setCalendarOpen(true),
  }));

  return (
    <>
      <RacTextField
        className="min-w-0"
        type="text"
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
            <div ref={anchorRef}>
              <Input
                ref={inputRef}
                placeholder={placeholder}
                isDisabled={isDisabled}
                isInvalid={isInvalid}
                addonLeft={
                  <InputButton
                    aria-label={t("inputDateRange.openCalendar")}
                    isDisabled={isDisabled}
                    onPress={() => setCalendarOpen(true)}
                  >
                    <CalendarIcon />
                  </InputButton>
                }
                addonRight={
                  // the input is content-sized, so an absent clear button would change its width
                  value ? (
                    <InputClearButton
                      isDisabled={isDisabled || isReadOnly}
                      onPress={() => {
                        onChange("");
                        inputRef.current?.focus();
                      }}
                    />
                  ) : (
                    <span className="size-6" />
                  )
                }
              />
            </div>
            <FieldError>{errorMessage}</FieldError>
          </>
        )}
      </RacTextField>
      {/* outside the text field, whose context would reach the calendar's inputs */}
      <Popover
        className="overflow-hidden"
        triggerRef={anchorRef}
        placement="bottom start"
        isOpen={isCalendarOpen}
        onOpenChange={setCalendarOpen}
      >
        <Dialog
          className="conquery-datepicker outline-none"
          aria-label={t("inputDateRange.calendar")}
        >
          <ReactDatePicker
            inline
            fixedHeight
            selected={value ? parseDate(value, dateFormat) : null}
            onChange={(val: Date | null) => {
              if (!val) {
                return;
              }

              const selectedDate = formatDate(val, dateFormat);
              onChange(selectedDate);
              setCalendarOpen(false);
              onCalendarSelect?.(selectedDate);
            }}
            renderCustomHeader={(props) => <CustomHeader {...props} />}
            calendarStartDay={1}
          />
        </Dialog>
      </Popover>
    </>
  );
};
