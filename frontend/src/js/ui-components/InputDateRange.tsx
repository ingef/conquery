import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { FC, ReactNode } from "react";
import { useTranslation } from "react-i18next";

import { IndexPrefix } from "../common/components/IndexPrefix";
import {
  formatDateFromState,
  parseDate,
  parseDateToState,
  getDateStringFromShortcut,
  DateStringMinMax,
} from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";

import BaseInput from "./BaseInput";
import Label from "./Label";
import Labeled from "./Labeled";
import Optional from "./Optional";

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

const SxLabeled = styled(Labeled)`
  &:first-of-type {
    margin-right: 10px;
  }
`;

interface PropsT {
  label?: ReactNode;
  indexPrefix?: number;
  labelSuffix?: ReactNode;
  className?: string;
  inline?: boolean;
  large?: boolean;
  center?: boolean;
  autoFocus?: boolean;
  tooltip?: string;
  optional?: boolean;
  value: DateStringMinMax;
  onChange: (value: DateStringMinMax) => void;
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

const InputDateRange: FC<PropsT> = ({
  large,
  inline,
  center,
  label,
  indexPrefix,
  autoFocus,
  labelSuffix,
  value,
  onChange,
  optional,
  tooltip,
}) => {
  const { t } = useTranslation();

  const onSetDate = (date: DateStringMinMax) => {
    onChange(date);
  };

  const onSetWhatDate = (what: "min" | "max", val: string) => {
    onChange({
      ...value,
      [what]: val,
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

  // To display the date depending on the locale
  const displayDateFormat = t("inputDateRange.dateFormat");

  const min = getDisplayDate("min", value, displayDateFormat);
  const max = getDisplayDate("max", value, displayDateFormat);

  const isMinValid = exists(value.min && parseDate(min, displayDateFormat));
  const isMaxValid = exists(value.max && parseDate(max, displayDateFormat));

  return (
    <Root center={center}>
      {label && (
        <StyledLabel large={large}>
          {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
          {optional && <Optional />}
          {label}
          <InfoTooltip
            html={
              <>
                {exists(tooltip) && (
                  <>
                    {tooltip}
                    <br />
                    <br />
                  </>
                )}
                <span
                  dangerouslySetInnerHTML={{
                    __html: t("inputDateRange.tooltip.possiblePattern"),
                  }}
                />
              </>
            }
          />
          {labelSuffix && labelSuffix}
        </StyledLabel>
      )}
      <Pickers inline={inline} center={center}>
        <SxLabeled label={t("inputDateRange.from")}>
          <BaseInput
            inputType="text"
            value={min}
            valid={isMinValid}
            invalid={min.length !== 0 && !isMinValid}
            invalidText={t("common.dateInvalid")}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={(val) =>
              onChangeRaw("min", val as string, displayDateFormat)
            }
            onBlur={(e) => applyDate("min", e.target.value, displayDateFormat)}
            inputProps={{
              autoFocus,
            }}
          />
        </SxLabeled>
        <SxLabeled label={t("inputDateRange.to")}>
          <BaseInput
            inputType="text"
            value={max}
            valid={isMaxValid}
            invalid={max.length !== 0 && !isMaxValid}
            invalidText={t("common.dateInvalid")}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={(val) =>
              onChangeRaw("max", val as string, displayDateFormat)
            }
            onBlur={(e) => applyDate("max", e.target.value, displayDateFormat)}
          />
        </SxLabeled>
      </Pickers>
    </Root>
  );
};

export default InputDateRange;
