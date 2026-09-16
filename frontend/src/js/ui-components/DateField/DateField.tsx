import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { type Ref, useRef } from "react";
import {
  TextField as RacTextField,
  type TextFieldProps as RacTextFieldProps,
} from "react-aria-components";
import ReactDatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { useTranslation } from "react-i18next";
import { mergeRefs } from "react-merge-refs";
import { tv } from "tailwind-variants";

import { formatDate, parseDate } from "../../common/helpers/dateHelper";
import { exists } from "../../common/helpers/exists";
import { FieldError } from "../FieldError";
import { Icon } from "../Icon";
import { Input, InputButton } from "../Input";
import { type FieldLabelProps, Label } from "../Label";
import { CustomHeader } from "./CustomHeader";

// react-datepicker's own elements are styled in index.css under this class
// and under #datepicker-portal (the popup renders into a body-level portal)
const picker = tv({ base: ["relative", "conquery-datepicker"] });

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
    /** the calendar, to open it from outside */
    ref?: Ref<ReactDatePicker>;
  };

/**
 * A date typed as text, with a calendar to pick it from. The text is the
 * value, so the caller can expand shortcuts like `q2.2020`.
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
  onKeyDown,
  ...props
}: DateFieldProps) => {
  const { t } = useTranslation();
  const datePickerRef = useRef<ReactDatePicker>(null);

  return (
    <RacTextField
      className="min-w-0"
      type="text"
      validationBehavior="aria"
      isInvalid={exists(errorMessage)}
      value={value}
      onChange={onChange}
      onKeyDown={(e) => {
        datePickerRef.current?.setOpen(false);
        onKeyDown?.(e);
      }}
      {...props}
    >
      {({ isDisabled, isInvalid }) => (
        <>
          {label && (
            <Label indexPrefix={indexPrefix} tooltip={tooltip}>
              {label}
            </Label>
          )}
          {/* biome-ignore lint/a11y/noStaticElementInteractions: escape inside the calendar closes it */}
          <div
            className={picker()}
            onKeyDown={(e) => {
              if (e.key === "Escape") datePickerRef.current?.setOpen(false);
            }}
          >
            <Input
              placeholder={placeholder}
              isDisabled={isDisabled}
              isInvalid={isInvalid}
              addonLeft={
                <InputButton
                  aria-label={t("inputDateRange.openCalendar")}
                  isDisabled={isDisabled}
                  onPress={() => datePickerRef.current?.setOpen(true)}
                >
                  <Icon icon={faCalendar} />
                </InputButton>
              }
            />
            <ReactDatePicker
              ref={mergeRefs([datePickerRef, ref])}
              portalId="datepicker-portal"
              selected={value ? parseDate(value, dateFormat) : new Date()}
              onChange={(val: Date | null) => {
                if (!val) {
                  return;
                }

                const selectedDate = formatDate(val, dateFormat);
                onChange(selectedDate);
                onCalendarSelect?.(selectedDate);
                datePickerRef.current?.setOpen(false);
              }}
              onClickOutside={() => datePickerRef.current?.setOpen(false)}
              renderCustomHeader={(props) => <CustomHeader {...props} />}
              customInput={<input className="hidden" />}
              calendarStartDay={1}
            />
          </div>
          <FieldError>{errorMessage}</FieldError>
        </>
      )}
    </RacTextField>
  );
};
