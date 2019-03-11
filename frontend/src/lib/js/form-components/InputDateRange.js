// @flow

import * as React from "react";
import styled from "@emotion/styled";

import T from "i18n-react";
import DatePicker, { registerLocale } from "react-datepicker";
import { type FieldPropsType } from "redux-form";

import { getDateLocale } from "../localization";
import {
  formatDate,
  parseDate,
  isValidDate,
  parseRawDate,
  parseDateFromShortcut
} from "../common/helpers/dateHelper";
import { InfoTooltip } from "../tooltip";

import Label from "./Label";
import Labeled from "./Labeled";

const Root = styled("div")`
  text-align: ${({ center }) => (center ? "center" : "left")};
`;
const Pickers = styled("div")`
  display: flex;
  flex-direction: ${({ inline }) => (inline ? "row" : "column")};
  justify-content: ${({ center }) => (center ? "center" : "flex-start")};
`;

const StyledLabeled = styled(Labeled)`
  &:first-of-type {
    margin-right: 10px;
    margin-bottom: 10px;
  }
`;

type PropsType = FieldPropsType & {
  label?: React.Node,
  className?: string,
  inline?: boolean,
  center?: boolean
};

const InputDateRange = (props: PropsType) => {
  React.useEffect(() => {
    const locale = getDateLocale();

    registerLocale("locale", locale);
  });

  const onSetDate = value => props.input.onChange(value);

  const onSetMinDate = date =>
    props.input.onChange({
      ...props.input.value,
      min: date
    });

  const onSetMaxDate = date =>
    props.input.onChange({
      ...props.input.value,
      max: date
    });

  const onChangeRawMin = (value, dateFormat) => {
    let { min, max } = parseDateFromShortcut(value);

    if (!min) min = parseRawDate(value);

    if (max) {
      onSetDate({
        min: formatDate(min, dateFormat),
        max: formatDate(max, dateFormat)
      });
    } else {
      onSetMinDate(formatDate(min, dateFormat));
    }
  };

  const onChangeRawMax = (value, dateFormat) => {
    let { max } = parseDateFromShortcut(value);

    if (!max) max = parseRawDate(value);

    onSetMaxDate(formatDate(max));
  };

  const {
    inline,
    center,
    label,
    input: { value }
  } = props;

  const dateFormat = T.translate("inputDateRange.dateFormat");

  const minDate = value ? parseDate(value.min, dateFormat) : null;
  const maxDate = value ? parseDate(value.max, dateFormat) : null;

  return (
    <Root center={center}>
      {label && (
        <Label>
          {label}
          <InfoTooltip
            text={T.translate("inputDateRange.tooltip.possiblePattern")}
          />
        </Label>
      )}
      <Pickers inline={inline} center={center}>
        <StyledLabeled label={T.translate("inputDateRange.from")}>
          <DatePicker
            id="datepicker-min"
            isClearable
            showYearDropdown
            scrollableYearDropdown
            tabIndex={1}
            selectsStart
            dateFormat={dateFormat}
            locale={"locale"}
            selected={minDate}
            startDate={minDate}
            endDate={maxDate}
            placeholderText={T.translate("inputDateRange.placeholder")}
            onChange={date => onSetMinDate(formatDate(date, dateFormat))}
            onChangeRaw={event =>
              onChangeRawMin(event.target.value, dateFormat)
            }
            ref={r => {
              if (r && minDate && isValidDate(minDate)) {
                r.setOpen(false);
                r.setSelected(minDate);
              }
            }}
          />
        </StyledLabeled>
        <StyledLabeled label={T.translate("inputDateRange.to")}>
          <DatePicker
            id="datepicker-max"
            isClearable
            showYearDropdown
            scrollableYearDropdown
            tabIndex={2}
            selectsEnd
            dateFormat={dateFormat}
            locale={"locale"}
            selected={maxDate}
            startDate={minDate}
            endDate={maxDate}
            placeholderText={T.translate("inputDateRange.placeholder")}
            onChange={date => onSetMaxDate(formatDate(date, dateFormat))}
            onChangeRaw={event =>
              onChangeRawMax(event.target.value, dateFormat)
            }
            ref={r => {
              if (r && maxDate && isValidDate(maxDate)) {
                r.setOpen(false);
                r.setSelected(maxDate);
              }
            }}
          />
        </StyledLabeled>
      </Pickers>
    </Root>
  );
};

export default InputDateRange;
