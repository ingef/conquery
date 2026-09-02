import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import type { DateRangeT } from "../../api/types";
import { formatDate } from "../../common/helpers/dateHelper";

const root = tv({
  base: [
    "inline-grid",
    "grid-cols-[auto_1fr]",
    "gap-x-[5px] gap-y-0",
    "text-xs",
    "font-mono",
  ],
});

const dateLabel = tv({
  base: ["justify-self-end", "uppercase", "text-primary-500", "font-bold"],
});

const getFormattedDate = (date: string | undefined, dateFormat: string) => {
  if (!date) return null;

  const d = new Date(date);

  if (Number.isNaN(d.getTime())) return null;

  return formatDate(d, dateFormat);
};

export const DateRange = ({ dateRange }: { dateRange: DateRangeT }) => {
  const { t } = useTranslation();
  const dateFormat = t("inputDateRange.dateFormat");

  const dateMin = getFormattedDate(dateRange.min, dateFormat);
  const dateMax = getFormattedDate(dateRange.max, dateFormat);

  return (
    <div className={root()}>
      {dateMin && (
        <>
          <div className={dateLabel()}>{t("inputDateRange.from")}</div>
          <span>{dateMin}</span>
        </>
      )}
      {dateMax && dateMax !== dateMin && (
        <>
          <div className={dateLabel()}>{t("inputDateRange.to")}</div>
          <span>{dateMax}</span>
        </>
      )}
    </div>
  );
};
