import { css } from "@emotion/react";
import styled from "@emotion/styled";
import React, { FC, ReactNode } from "react";
import { useTranslation } from "react-i18next";

import {
  formatDateFromState,
  parseDate,
  parseDateToState,
  getDateStringFromShortcut,
  DateStringMinMax,
} from "../common/helpers/dateHelper";
import InfoTooltip from "../tooltip/InfoTooltip";

import BaseInput from "./BaseInput";
import Label from "./Label";
import Labeled from "./Labeled";

const Root = styled("div")<{ center?: boolean }>`
  text-align: ${({ center }) => (center ? "center" : "left")};
`;
const Pickers = styled("div")<{ inline?: boolean; center?: boolean }>`
  display: flex;
  flex-direction: ${({ inline }) => (inline ? "row" : "column")};
  justify-content: ${({ center }) => (center ? "center" : "flex-start")};
`;

const StyledLabel = styled(Label)<{ large?: boolean }>`
  ${({ theme, large }) =>
    large &&
    css`
      font-size: ${theme.font.md};
      margin: 20px 0 10px;
    `}
`;

const StyledLabeled = styled(Labeled)`
  &:first-of-type {
    margin-right: 10px;
    margin-bottom: 10px;
  }
`;

interface PropsT {
  label?: ReactNode;
  labelSuffix?: ReactNode;
  className?: string;
  inline?: boolean;
  large?: boolean;
  center?: boolean;
  input: {
    value: DateStringMinMax;
    onChange: (value: DateStringMinMax) => void;
  };
}

function getDisplayDate(
  what: "min" | "max",
  value: DateStringMinMax,
  dateFormat: string,
) {
  const dateString = value[what];

  if (!dateString) return "";

  return formatDateFromState(dateString, dateFormat);
}

const InputDateRange: FC<PropsT> = (props) => {
  const { t } = useTranslation();

  const onSetDate = (date: DateStringMinMax) => {
    props.input.onChange(date);
  };

  const onSetWhatDate = (what: "min" | "max", value: string) => {
    props.input.onChange({
      ...props.input.value,
      [what]: value,
    });
  };

  const onChangeRaw = (
    what: "min" | "max",
    val: string,
    dateFormat: string,
  ) => {
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

  const applyDate = (what: "min" | "max", val: string, dateFormat: string) => {
    if (parseDate(val, dateFormat) === null) {
      onSetWhatDate(what, "");
    }
  };

  const {
    large,
    inline,
    center,
    label,
    labelSuffix,
    input: { value },
  } = props;

  // To display the date depending on the locale
  const displayDateFormat = t("inputDateRange.dateFormat");

  const min = getDisplayDate("min", value, displayDateFormat);
  const max = getDisplayDate("max", value, displayDateFormat);

  return (
    <Root center={center}>
      {label && (
        <StyledLabel large={large}>
          {label}
          <InfoTooltip text={t("inputDateRange.tooltip.possiblePattern")} />
          {labelSuffix && labelSuffix}
        </StyledLabel>
      )}
      <Pickers inline={inline} center={center}>
        <StyledLabeled label={t("inputDateRange.from")}>
          <BaseInput
            inputType="text"
            value={min}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={(val) =>
              onChangeRaw("min", val as string, displayDateFormat)
            }
            onBlur={(e) => applyDate("min", e.target.value, displayDateFormat)}
            inputProps={{
              autoFocus: true,
            }}
          />
        </StyledLabeled>
        <StyledLabeled label={t("inputDateRange.to")}>
          <BaseInput
            inputType="text"
            value={max}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={(val) =>
              onChangeRaw("max", val as string, displayDateFormat)
            }
            onBlur={(e) => applyDate("max", e.target.value, displayDateFormat)}
          />
        </StyledLabeled>
      </Pickers>
    </Root>
  );
};

export default InputDateRange;
