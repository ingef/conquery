import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { type Ref, useRef } from "react";
import ReactDatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { mergeRefs } from "react-merge-refs";
import { tv } from "tailwind-variants";

import IconButton from "../../button/IconButton";
import { formatDate, parseDate } from "../../common/helpers/dateHelper";
import BaseInput, { type Props as BaseInputProps } from "../BaseInput";

import { CustomHeader } from "./CustomHeader";

// react-datepicker's own elements are styled in index.css under this class
// and under #datepicker-portal (the popup renders into a body-level portal)
const root = tv({
  base: ["relative", "conquery-datepicker"],
});

const calendarButton = tv({
  base: ["absolute top-0 left-0", "px-[10px] py-2"],
});

const baseInput = tv({
  base: "[&_input]:pl-[28px]",
});

type Props = Omit<BaseInputProps, "inputType"> & {
  value: string | null;
  dateFormat: string;
  className?: string;
  onChange: (val: string) => void;
  onCalendarSelect?: (val: string) => void;
};

const InputDate = ({
  ref,
  className,
  value,
  dateFormat,
  onChange,
  onCalendarSelect,
  ...props
}: Props & { ref?: Ref<ReactDatePicker> }) => {
  const datePickerRef = useRef<ReactDatePicker>(null);

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: escape closes the calendar of the input inside
    <div
      className={root({ className })}
      onKeyDown={(e) => {
        if (e.key === "Escape") datePickerRef.current?.setOpen(false);
      }}
    >
      <BaseInput
        {...props}
        className={baseInput()}
        inputType="text"
        value={value}
        onChange={(val) => {
          onChange(val as string);
        }}
        inputProps={{
          ...props?.inputProps,
          onKeyPress: (e) => {
            datePickerRef.current?.setOpen(false);
            props.inputProps?.onKeyPress?.(e);
          },
        }}
      />
      <IconButton
        className={calendarButton()}
        icon={faCalendar}
        onClick={() => datePickerRef.current?.setOpen(true)}
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
        renderCustomHeader={CustomHeader}
        customInput={<input className="hidden" />}
        calendarStartDay={1}
      />
    </div>
  );
};

export default InputDate;
