import { CalendarIcon } from "lucide-react";
import { type ReactNode, useId, useRef } from "react";
import { Group } from "react-aria-components";
import type ReactDatePicker from "react-datepicker";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import {
  type DateStringMinMax,
  formatDateFromState,
  getDateStringFromShortcut,
  parseDate,
  parseDateToState,
} from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import { DateField } from "./DateField/DateField";
import InfoTooltip from "./InfoTooltip";
import { Label } from "./Label";
import { C2 } from "./Typography";

const customTooltip = tv({
  base: [
    "flex flex-col",
    "gap-2",
    "px-[14px] py-2",
    "[&_table]:mt-[5px] [&_table]:w-full",
    "[&_table]:border [&_th]:border [&_td]:border",
    "[&_table]:border-gray-100 [&_th]:border-gray-100 [&_td]:border-gray-100",
    "[&_table]:border-collapse [&_th]:border-collapse [&_td]:border-collapse",
    "[&_td]:px-[5px] [&_td]:py-[2px]",
  ],
});

function getDisplayDate(
  what: "min" | "max",
  value: DateStringMinMax,
  dateFormat: string,
) {
  const dateString = value[what];

  if (!dateString) return "";

  return formatDateFromState(dateString, dateFormat);
}

export const DateRangeField = ({
  label,
  indexPrefix,
  autoFocus,
  labelSuffix,
  isDisabled,
  value,
  onChange,
  tooltip,
}: {
  label?: ReactNode;
  indexPrefix?: number;
  labelSuffix?: ReactNode;
  autoFocus?: boolean;
  isDisabled?: boolean;
  tooltip?: string;
  value: DateStringMinMax;
  onChange: (value: DateStringMinMax) => void;
}) => {
  const { t } = useTranslation();
  const labelId = useId();

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

  const maxRef = useRef<ReactDatePicker>(null);

  const isMinValid = exists(value.min && parseDate(min, displayDateFormat));
  const isMaxValid = exists(value.max && parseDate(max, displayDateFormat));

  return (
    <Group aria-labelledby={label ? labelId : undefined}>
      {label && (
        <Label
          id={labelId}
          elementType="span"
          indexPrefix={indexPrefix}
          isDisabled={isDisabled}
        >
          <CalendarIcon className="mr-[10px] text-gray-500" />
          {label}
          <InfoTooltip
            excludeFromTabOrder
            html={
              <div className={customTooltip()}>
                {exists(tooltip) && <C2>{tooltip}</C2>}
                <C2
                  // biome-ignore lint/security/noDangerouslySetInnerHtml: i18n text with markup
                  dangerouslySetInnerHTML={{
                    __html: t("inputDateRange.tooltip.possiblePattern"),
                  }}
                />
              </div>
            }
          />
          {labelSuffix}
        </Label>
      )}
      <div className="flex items-start gap-[5px]">
        <DateField
          aria-label={t("inputDateRange.from")}
          isDisabled={isDisabled}
          value={min}
          dateFormat={displayDateFormat}
          errorMessage={
            min.length !== 0 && !isMinValid
              ? t("common.dateInvalid")
              : undefined
          }
          placeholder={displayDateFormat.toUpperCase()}
          onChange={(val) => onChangeRaw("min", val, displayDateFormat)}
          onCalendarSelect={() => maxRef.current?.setOpen(true)}
          onBlur={(e) => applyDate("min", e.target.value, displayDateFormat)}
          autoFocus={autoFocus}
        />
        <span aria-hidden className="flex h-[30px] items-center text-gray-600">
          –
        </span>
        <DateField
          ref={maxRef}
          aria-label={t("inputDateRange.to")}
          isDisabled={isDisabled}
          value={max}
          dateFormat={displayDateFormat}
          errorMessage={
            max.length !== 0 && !isMaxValid
              ? t("common.dateInvalid")
              : undefined
          }
          placeholder={displayDateFormat.toUpperCase()}
          onChange={(val) => onChangeRaw("max", val, displayDateFormat)}
          onBlur={(e) => applyDate("max", e.target.value, displayDateFormat)}
        />
      </div>
    </Group>
  );
};
