import {
  faArrowsLeftRightToLine,
  faHashtag,
  faMicroscope,
  faUser,
} from "@fortawesome/free-solid-svg-icons";
import type { HTMLAttributes } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { DateRangeT } from "../api/types";
import { numberToThreeDigitArray } from "../common/helpers/commonHelper";
import { formatDate, parseDate } from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import FaIcon from "../icon/FaIcon";

const dateText = tv({
  base: [
    "m-0",
    "pr-2",
    "font-bold",
    "text-sm",
    "flex items-center",
    "whitespace-nowrap",
  ],
});

const text = tv({
  base: ["m-0", "text-xs", "uppercase", "font-normal"],
  variants: {
    zero: {
      true: "text-red",
      false: "text-gray-500",
    },
  },
});

const icon = tv({
  base: ["text-[30px]", "text-gray-400", "justify-self-center"],
});

const numberText = tv({
  base: ["font-bold", "m-0", "text-xl", "leading-none"],
  variants: {
    zero: { true: "text-red" },
  },
});

const digits = tv({
  base: [
    "after:text-gray-500",
    "after:content-['.']",
    "last-of-type:after:content-['']",
  ],
});

const suffix = tv({
  base: ["text-gray-500", "font-normal", "uppercase", "text-xs", "ml-[5px]"],
});

interface Props extends HTMLAttributes<HTMLDivElement> {
  matchingEntries?: number | null;
  matchingEntities?: number | null;
  dateRange?: DateRangeT;
  idLabel?: string;
}

const TooltipEntries = (props: Props) => {
  const { t } = useTranslation();
  const { matchingEntries, matchingEntities, dateRange, idLabel, ...rest } =
    props;

  const isZero = props.matchingEntries === 0;
  const isZeroEntities = props.matchingEntities === 0;

  const dateFormat = "yyyy-MM-dd";
  const displayDateFormat = t("inputDateRange.dateFormat");

  const parsedFromDate = dateRange?.min
    ? parseDate(dateRange.min, dateFormat)
    : null;
  const fromDate = parsedFromDate
    ? formatDate(parsedFromDate, displayDateFormat)
    : "- - - - - - -";

  const parsedToDate = dateRange?.max
    ? parseDate(dateRange.max, dateFormat)
    : null;
  const toDate = parsedToDate
    ? formatDate(parsedToDate, displayDateFormat)
    : "- - - - - - -";

  return (
    <div {...rest}>
      {idLabel && (
        <>
          <FaIcon className={icon()} icon={faMicroscope} />
          <div className="shrink-0">
            <p className={dateText()}>{idLabel}</p>
            <p className={text({ zero: isZero })}>
              {t("queryEditor.secondaryId")}
            </p>
          </div>
        </>
      )}
      <FaIcon className={icon()} icon={faHashtag} />
      <div className="shrink-0">
        <p className={numberText({ zero: isZero })}>
          {exists(matchingEntries) ? (
            numberToThreeDigitArray(matchingEntries).map((threeDigits, i) => (
              <span className={digits()} key={i}>
                {threeDigits}
              </span>
            ))
          ) : (
            <span className={digits()}>-</span>
          )}
        </p>
        <p className={text({ zero: isZero })}>
          {t(
            "tooltip.entriesFound",
            { count: matchingEntries || 2 }, // For pluralization
          )}
        </p>
      </div>
      <FaIcon className={icon()} icon={faUser} />
      <div className="shrink-0">
        <p className={numberText({ zero: isZeroEntities })}>
          {exists(matchingEntities) ? (
            numberToThreeDigitArray(matchingEntities).map((threeDigits, i) => (
              <span className={digits()} key={i}>
                {threeDigits}
              </span>
            ))
          ) : (
            <span className={digits()}>-</span>
          )}
        </p>
        <p className={text({ zero: isZeroEntities })}>
          {t(
            "tooltip.entitiesFound",
            { count: matchingEntities || 2 }, // For pluralization
          )}
        </p>
      </div>
      <FaIcon className={icon()} icon={faArrowsLeftRightToLine} />
      <div className="shrink-0">
        <p className={dateText()}>
          {fromDate}
          <span className={suffix()}>{`${t("tooltip.date.from")}`}</span>
        </p>
        <p className={dateText()}>
          {toDate}
          <span className={suffix()}>{`${t("tooltip.date.to")}`}</span>
        </p>
      </div>
    </div>
  );
};

export default TooltipEntries;
