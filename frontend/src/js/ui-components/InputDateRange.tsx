import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { FC, ReactNode, createRef, useMemo } from "react";
import ReactDatePicker from "react-datepicker";
import { useTranslation } from "react-i18next";

import { IndexPrefix } from "../common/components/IndexPrefix";
import {
  DateStringMinMax,
  formatDateFromState,
  getDateStringFromShortcut,
  parseDate,
  parseDateToState,
} from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import { Icon } from "../icon/FaIcon";
import InfoTooltip from "../tooltip/InfoTooltip";

import InputDate from "./InputDate/InputDate";
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
    `}
`;

const SxLabeled = styled(Labeled)`
  &:first-of-type {
    margin-right: 10px;
  }
`;

const CustomTooltip = styled("div")`
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 14px;

  font-size: ${({ theme }) => theme.font.md};
  font-weight: 400;
  table {
    margin-top: 5px;
    width: 100%;
  }
  table,
  th,
  td {
    border: 1px solid ${({ theme }) => theme.col.grayLight};
    border-collapse: collapse;
  }
  td {
    padding: 2px 5px;
    line-height: 1.2;
  }
`;

const TooltipMain = styled("div")`
  font-size: ${({ theme }) => theme.font.md};
`;

const TooltipTutorial = styled("div")<{ hasMain?: boolean }>`
  font-size: ${({ theme, hasMain }) =>
    hasMain ? theme.font.sm : theme.font.md};
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

  const maxRef = createRef<ReactDatePicker>();

  const isMinValid = exists(value.min && parseDate(min, displayDateFormat));
  const isMaxValid = exists(value.max && parseDate(max, displayDateFormat));

  const labelWithSuffix = useMemo(() => {
    if (!label) return null;

    return (
      <StyledLabel large={large}>
        <Icon icon={faCalendar} left gray />
        {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
        {label}
        <InfoTooltip
          html={
            <CustomTooltip>
              {exists(tooltip) && <TooltipMain>{tooltip}</TooltipMain>}
              <TooltipTutorial
                hasMain={exists(tooltip)}
                dangerouslySetInnerHTML={{
                  __html: t("inputDateRange.tooltip.possiblePattern"),
                }}
              />
            </CustomTooltip>
          }
        />
        {labelSuffix && labelSuffix}
      </StyledLabel>
    );
  }, [t, label, labelSuffix, large, tooltip, indexPrefix]);

  return (
    <Root center={center}>
      {labelWithSuffix}
      <Pickers inline={inline} center={center}>
        <SxLabeled label={t("inputDateRange.from")}>
          <InputDate
            value={min}
            dateFormat={displayDateFormat}
            valid={isMinValid}
            invalid={min.length !== 0 && !isMinValid}
            invalidText={t("common.dateInvalid")}
            placeholder={displayDateFormat.toUpperCase()}
            onChange={(val) =>
              onChangeRaw("min", val as string, displayDateFormat)
            }
            onCalendarSelect={() => maxRef.current?.setOpen(true)}
            onBlur={(e) => applyDate("min", e.target.value, displayDateFormat)}
            inputProps={{
              autoFocus,
            }}
          />
        </SxLabeled>
        <SxLabeled label={t("inputDateRange.to")}>
          <InputDate
            ref={maxRef}
            value={max}
            dateFormat={displayDateFormat}
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
