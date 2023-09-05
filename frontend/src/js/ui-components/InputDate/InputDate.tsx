import styled from "@emotion/styled";
import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { createElement, forwardRef, useRef } from "react";
import ReactDatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import { mergeRefs } from "react-merge-refs";

import IconButton from "../../button/IconButton";
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
  }
  .react-datepicker-popper[data-placement^="top"] {
    padding-bottom: 0;
  }
`;

const CalendarIcon = styled(IconButton)`
  position: absolute;
  width: 16px;
  height: 16px;
  top: calc(50% - 8px);
  left: 5px;
  padding: 0;
`;

const StyledBaseInput = styled(BaseInput)`
  input {
    padding-left: 28px;
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

const InputDate = forwardRef<ReactDatePicker, Props>(
  (
    { className, value, dateFormat, onChange, onCalendarSelect, ...props },
    ref,
  ) => {
    const datePickerRef = useRef<ReactDatePicker>(null);

    return (
      <Root
        className={className}
        onKeyDown={(e) => {
          if (e.key === "Escape") datePickerRef.current?.setOpen(false);
        }}
      >
        <StyledBaseInput
          {...props}
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
        <CalendarIcon
          icon={faCalendar}
          iconProps={{ style: { width: "16px", height: "16px" } }}
          onClick={() => datePickerRef.current?.setOpen(true)}
        />
        <ReactDatePicker
          ref={mergeRefs([datePickerRef, ref])}
          selected={value ? parseDate(value, dateFormat) : new Date()}
          onChange={(val) => {
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
          customInput={createElement(HiddenInput)}
          calendarContainer={StyledCalendar}
          calendarStartDay={1}
          popperProps={{
            modifiers: [
              {
                name: "preventOverflow",
                options: {
                  mainAxis: false,
                },
              },
            ],
          }}
        />
      </Root>
    );
  },
);

export default InputDate;
