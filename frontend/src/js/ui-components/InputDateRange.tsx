import { faCalendar } from "@fortawesome/free-regular-svg-icons";
import { createRef, type ReactNode, useMemo } from "react";
import type ReactDatePicker from "react-datepicker";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { IndexPrefix } from "../common/components/IndexPrefix";
import {
  type DateStringMinMax,
  formatDateFromState,
  getDateStringFromShortcut,
  parseDate,
  parseDateToState,
} from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";
import InfoTooltip from "./InfoTooltip";

import InputDate from "./InputDate/InputDate";
import Label from "./Label";
import Labeled from "./Labeled";

const root = tv({
  variants: {
    center: {
      true: "text-center",
      false: "text-left",
    },
  },
});

const pickers = tv({
  base: "flex",
  variants: {
    inline: {
      true: "flex-row",
      false: "flex-col",
    },
    center: {
      true: "justify-center",
      false: "justify-start",
    },
  },
});

const labeled = tv({
  base: "first-of-type:mr-[10px]",
});

const customTooltip = tv({
  base: [
    "flex flex-col",
    "gap-2",
    "px-[14px] py-2",
    "text-base",
    "font-normal",
    "[&_table]:mt-[5px] [&_table]:w-full",
    "[&_table]:border [&_th]:border [&_td]:border",
    "[&_table]:border-gray-100 [&_th]:border-gray-100 [&_td]:border-gray-100",
    "[&_table]:border-collapse [&_th]:border-collapse [&_td]:border-collapse",
    "[&_td]:px-[5px] [&_td]:py-[2px]",
    "[&_td]:leading-[1.2]",
  ],
});

const tooltipMain = tv({
  base: "text-base",
});

const tooltipTutorial = tv({
  variants: {
    hasMain: {
      true: "text-sm",
      false: "text-base",
    },
  },
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

const InputDateRange = ({
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
}: {
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
      <Label large={large}>
        <FaIcon icon={faCalendar} left gray />
        {exists(indexPrefix) && <IndexPrefix># {indexPrefix}</IndexPrefix>}
        {label}
        <InfoTooltip
          html={
            <div className={customTooltip()}>
              {exists(tooltip) && (
                <div className={tooltipMain()}>{tooltip}</div>
              )}
              <div
                className={tooltipTutorial({ hasMain: exists(tooltip) })}
                // biome-ignore lint/security/noDangerouslySetInnerHtml: i18n text with markup
                dangerouslySetInnerHTML={{
                  __html: t("inputDateRange.tooltip.possiblePattern"),
                }}
              />
            </div>
          }
        />
        {labelSuffix && labelSuffix}
      </Label>
    );
  }, [t, label, labelSuffix, large, tooltip, indexPrefix]);

  return (
    <div className={root({ center: !!center })}>
      {labelWithSuffix}
      <div className={pickers({ inline: !!inline, center: !!center })}>
        <Labeled className={labeled()} label={t("inputDateRange.from")}>
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
        </Labeled>
        <Labeled className={labeled()} label={t("inputDateRange.to")}>
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
        </Labeled>
      </div>
    </div>
  );
};

export default InputDateRange;
