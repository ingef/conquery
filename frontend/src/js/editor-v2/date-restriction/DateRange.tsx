import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { DateRangeT } from "../../api/types";
import { formatDate } from "../../common/helpers/dateHelper";
import { C3 } from "../../ui-components/Typography";

const root = tv({
  base: ["inline-grid", "grid-cols-[auto_1fr]", "gap-x-[5px] gap-y-0"],
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
          <span className="justify-self-end">
            <C3 as="span" tone="primary" strong>
              {t("inputDateRange.from")}
            </C3>
          </span>
          <C3 as="code">{dateMin}</C3>
        </>
      )}
      {dateMax && dateMax !== dateMin && (
        <>
          <span className="justify-self-end">
            <C3 as="span" tone="primary" strong>
              {t("inputDateRange.to")}
            </C3>
          </span>
          <C3 as="code">{dateMax}</C3>
        </>
      )}
    </div>
  );
};
