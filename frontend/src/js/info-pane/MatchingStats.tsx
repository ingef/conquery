import {
  HashIcon,
  MicroscopeIcon,
  UnfoldHorizontalIcon,
  UserIcon,
} from "lucide-react";
import type { HTMLAttributes } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { DateRangeT } from "../api/types";
import { numberToThreeDigitArray } from "../common/helpers/commonHelper";
import { formatDate, parseDate } from "../common/helpers/dateHelper";
import { exists } from "../common/helpers/exists";
import { headingStyle, textStyle } from "../ui-components/Typography";

const dateText = tv({
  base: [
    "pr-2",
    textStyle({ size: 2, strong: true }),
    "flex items-center",
    "whitespace-nowrap",
  ],
});

const text = tv({
  base: textStyle({ size: 3 }),
  variants: {
    zero: {
      true: "text-red",
      false: "text-gray-600",
    },
  },
});

const icon = tv({
  base: ["size-[30px]", "text-gray-400", "justify-self-center"],
});

const numberText = tv({
  base: headingStyle({ level: 3 }),
  variants: {
    zero: { true: "text-red" },
  },
});

const digits = tv({
  base: [
    "after:text-gray-600",
    "after:content-['.']",
    "last-of-type:after:content-['']",
  ],
});

const suffix = tv({
  base: [textStyle({ size: 3, tone: "muted" }), "ml-[5px]"],
});

interface Props extends HTMLAttributes<HTMLDivElement> {
  matchingEntries?: number | null;
  matchingEntities?: number | null;
  dateRange?: DateRangeT;
  idLabel?: string;
}

const MatchingStats = (props: Props) => {
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
          <MicroscopeIcon className={icon()} />
          <div className="shrink-0">
            <p className={dateText()}>{idLabel}</p>
            <p className={text({ zero: isZero })}>
              {t("queryEditor.secondaryId")}
            </p>
          </div>
        </>
      )}
      <HashIcon className={icon()} />
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
            "common.entriesFound",
            { count: matchingEntries || 2 }, // For pluralization
          )}
        </p>
      </div>
      <UserIcon className={icon()} />
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
            "common.entitiesFound",
            { count: matchingEntities || 2 }, // For pluralization
          )}
        </p>
      </div>
      <UnfoldHorizontalIcon className={icon()} />
      <div className="shrink-0">
        <p className={dateText()}>
          {fromDate}
          <span className={suffix()}>{`${t("infoPane.date.from")}`}</span>
        </p>
        <p className={dateText()}>
          {toDate}
          <span className={suffix()}>{`${t("infoPane.date.to")}`}</span>
        </p>
      </div>
    </div>
  );
};

export default MatchingStats;
