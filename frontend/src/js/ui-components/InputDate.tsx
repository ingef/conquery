import styled from "@emotion/styled";
import { createElement, createRef, forwardRef, useState } from "react";
import ReactDatePicker from "react-datepicker";

import { formatDate, parseDate } from "../common/helpers/dateHelper";

import BaseInput, { Props as BaseInputProps } from "./BaseInput";

const Root = styled("div")`
  position: relative;

  .react-datepicker-wrapper {
    position: absolute;
    top: 0;
    bottom: 0;
    z-index: -1;
  }
  .react-datepicker-popper[data-placement^="bottom"] {
    padding-top: 0.25rem;
    transform: translate3d(0, 2rem, 0) !important;
  }
  .react-datepicker-popper[data-placement^="top"] {
    padding-bottom: 0;
    transform: translate3d(0, -2rem, 0) !important;
  }
`;

const HiddenInput = styled("input")`
  display: none;
`;

const StyledCalendar = styled("div")`
  .react-datepicker__day--selected {
    background: ${({ theme }) => theme.col.green};
  }
`;

type Props = Omit<BaseInputProps, "inputType"> & {
  value: string | null;
  dateFormat: string;
  className?: string;
  onChange: (val: string | null) => void;
  onCalendarSelect?: () => void;
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
      ...props
    },
    ref,
  ) => {
    const datePickerRef = createRef<ReactDatePicker>();
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
            val && onChange(formatDate(val as Date, dateFormat));
            onCalendarSelect?.();
            setFocusBlocked(true);
            datePickerRef.current?.setOpen(false);
          }}
          onClickOutside={() => datePickerRef.current?.setOpen(false)}
          customInput={createElement(HiddenInput)}
          calendarContainer={StyledCalendar}
        />
      </Root>
    );
  },
);

export default InputDate;
