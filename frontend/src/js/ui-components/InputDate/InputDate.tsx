import styled from "@emotion/styled";
import { createElement, forwardRef, useRef, useState } from "react";
import ReactDatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";

import { formatDate, parseDate } from "../../common/helpers/dateHelper";
import BaseInput, { Props as BaseInputProps } from "../BaseInput";

import { CustomHeader } from "./CustomHeader";

const Root = styled("div")`
  position: relative;

  .react-datepicker-wrapper {
    position: absolute;
    top: 0;
    bottom: 0;
    z-index: -1;
  }
  .react-datepicker-popper[data-placement^="bottom"] {
    padding-top: 4px;
    transform: translate3d(0, 32px, 0) !important;
  }
  .react-datepicker-popper[data-placement^="top"] {
    padding-bottom: 0;
    transform: translate3d(0, 32px, 0) !important;
  }
`;

const HiddenInput = styled("input")`
  display: none;
`;

const StyledCalendar = styled("div")`
  .react-datepicker__day--selected {
    background: ${({ theme }) => theme.col.blueGrayDark};
  }
`;

type Props = Omit<BaseInputProps, "inputType"> & {
  value: string | null;
  dateFormat: string;
  className?: string;
  onChange: (val: string) => void;
  onCalendarSelect?: (val: string) => void;
};

const InputDate = forwardRef<HTMLInputElement, Props>(
  (
    {
      className,
      value,
      dateFormat,
      onChange,
      onCalendarSelect,
      onFocus,
      onBlur,
      onClick,
      ...props
    },
    ref,
  ) => {
    const datePickerRef = useRef<ReactDatePicker>(null);
    const [hasFocus, setHasFocus] = useState(false);
    const [focusBlocked, setFocusBlocked] = useState(false);

    return (
      <Root
        className={className}
        onKeyDown={(e) => {
          if (e.key === "Escape") datePickerRef.current?.setOpen(false);
        }}
      >
        <BaseInput
          {...props}
          ref={ref}
          inputType="text"
          value={value}
          onChange={(val) => {
            onChange(val as string);
          }}
          onFocus={(e) => {
            if (focusBlocked) {
              e.currentTarget.blur();
              setFocusBlocked(false);
            } else {
              onFocus?.(e);
              setHasFocus(true);
              datePickerRef.current?.setOpen(true);
            }
          }}
          onBlur={(e) => {
            onBlur?.(e);
            setHasFocus(false);
          }}
          onClick={(e) => {
            onClick?.(e);
            if (hasFocus) {
              datePickerRef.current?.setOpen(true);
            }
          }}
          inputProps={{
            ...props?.inputProps,
            onKeyPress: (e) => {
              datePickerRef.current?.setOpen(false);
              props.inputProps?.onKeyPress?.(e);
            },
          }}
        />
        <ReactDatePicker
          ref={datePickerRef}
          selected={value ? parseDate(value, dateFormat) : new Date()}
          onChange={(val) => {
            if (!val) {
              return;
            }

            const selectedDate = formatDate(val, dateFormat);
            onChange(selectedDate);
            onCalendarSelect?.(selectedDate);
            setFocusBlocked(true);
            datePickerRef.current?.setOpen(false);
          }}
          onClickOutside={() => datePickerRef.current?.setOpen(false)}
          renderCustomHeader={CustomHeader}
          customInput={createElement(HiddenInput)}
          calendarContainer={StyledCalendar}
          showFullMonthYearPicker={true}
        />
      </Root>
    );
  },
);

export default InputDate;
