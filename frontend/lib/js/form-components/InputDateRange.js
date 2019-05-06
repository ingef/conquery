// @flow

import * as React from "react";
import styled from "@emotion/styled";

import T from "i18n-react";
import { type FieldPropsType } from "redux-form";

import {
  formatDateFromState,
  parseDate,
  parseDateToState,
  getDateStringFromShortcut
} from "../common/helpers/dateHelper";
import InfoTooltip from "../tooltip/InfoTooltip";

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

const StyledLabel = styled(Label)`
  font-size: ${({ theme }) => theme.font.md};
  margin-bottom: 10px;
`;

const StyledLabeled = styled(Labeled)`
  &:first-of-type {
    margin-right: 10px;
    margin-bottom: 10px;
  }
`;

type PropsType = FieldPropsType & {
  label?: React.Node,
  labelSuffix?: React.Node,
  className?: string,
  inline?: boolean,
  center?: boolean
};

function getDisplayDate(what, value, displayDateFormat) {
  if (!value || !value[what]) return "";

  return formatDateFromState(value[what], displayDateFormat);
}

const InputDateRange = (props: PropsType) => {
  const onSetDate = date => {
    props.input.onChange(date);
  };

  const onSetWhatDate = (what, value) => {
    props.input.onChange({
      ...props.input.value,
      [what]: value
    });
  };

  const onChangeRaw = (what, val, dateFormat) => {
    const potentialDate = parseDate(val, dateFormat);

    if (potentialDate) {
      return onSetWhatDate(what, parseDateToState(potentialDate));
    }

    const { min, max } = getDateStringFromShortcut(what, val, dateFormat);

    if (min && max) {
      onSetDate({ min, max });
    } else if (min) {
      onSetWhatDate("min", min);
    } else if (max) {
      onSetWhatDate("max", max);
    } else {
      onSetWhatDate(what, val);
    }
  };

  const applyDate = (what, val, displayDateFormat) => {
    if (parseDate(val, displayDateFormat) === null) {
      onSetWhatDate(what, "");
    }
  };

  const {
    inline,
    center,
    label,
    labelSuffix,
    input: { value }
  } = props;

  // To display the date depending on the locale
  const displayDateFormat = T.translate("inputDateRange.dateFormat");

  const min = getDisplayDate("min", value, displayDateFormat);
  const max = getDisplayDate("max", value, displayDateFormat);

  return (
    <Root center={center}>
      {label && (
        <StyledLabel>
          {label}
          <InfoTooltip
            text={T.translate("inputDateRange.tooltip.possiblePattern")}
          />
          {labelSuffix && labelSuffix}
        </StyledLabel>
      )}
      <Pickers inline={inline} center={center}>
        <StyledLabeled label={T.translate("inputDateRange.from")}>
          <input
            tabIndex={1}
            value={min}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={event =>
              onChangeRaw("min", event.target.value, displayDateFormat)
            }
            onBlur={e => applyDate("min", e.target.value, displayDateFormat)}
          />
        </StyledLabeled>
        <StyledLabeled label={T.translate("inputDateRange.to")}>
          <input
            tabIndex={2}
            value={max}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={event =>
              onChangeRaw("max", event.target.value, displayDateFormat)
            }
            onBlur={e => applyDate("max", e.target.value, displayDateFormat)}
          />
        </StyledLabeled>
      </Pickers>
    </Root>
  );
};

export default InputDateRange;
