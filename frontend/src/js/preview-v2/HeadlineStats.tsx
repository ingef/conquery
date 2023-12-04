import styled from "@emotion/styled";
import { t } from "i18next";
import { PreviewStatisticsResponse } from "../api/types";
import { getDiffInDays, parseDate } from "../common/helpers/dateHelper";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  gap: 15px;
  padding: 10px;
  justify-content: right;
`;

const MetaValue = styled("div")`
  display: flex;
  flex-direction: row;
  gap: 5px;
`;

const Key = styled("span")`
  font-weight: bold;
  margin-left: 10px;

  &:first-of-type {
    margin-left: 0;
  }
`;

export type HeadlineStatsProps = {
  statistics: PreviewStatisticsResponse | null;
};

export default function HeadlineStats({ statistics }: HeadlineStatsProps) {
  const parseDateToLocaleString = (date: string | undefined) => {
    if (date) {
      return (
        parseDate(date, "yyyy-MM-dd")?.toLocaleDateString("de-de") ??
        t("preview.dateError")
      );
    }
    return t("preview.dateError");
  };
  const dateRange = statistics?.dateRange;
  
  return (
    <Root>
      <MetaValue>
        <Key>Zeilen:</Key>
        {statistics?.total || "Noch nicht geladen"}
      </MetaValue>
      <MetaValue>
        <Key>Min Datum:</Key>
        {parseDateToLocaleString(statistics?.dateRange.min)}
      </MetaValue>
      <MetaValue>
        <Key>Max Datum:</Key>
        {parseDateToLocaleString(dateRange?.max)}
      </MetaValue>
      <MetaValue>
        <Key>Datumsbereich:</Key>
        {dateRange?.min && dateRange?.max
          ? `${getDiffInDays(
              parseDate(dateRange.max, "yyyy-MM-dd") ?? new Date(),
              parseDate(dateRange.min, "yyyy-MM-dd") ?? new Date(),
            )} ${t("common.timeUnitDays")}`
          : t("preview.dateError")}
      </MetaValue>
    </Root>
  );
}
