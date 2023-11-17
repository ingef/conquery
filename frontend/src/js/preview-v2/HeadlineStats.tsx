import styled from "@emotion/styled";
import { PreviewStatisticsResponse } from "../api/types";
import { getDiffInDays, parseDate } from "../common/helpers/dateHelper";
import { t } from "i18next";

const Root = styled("div")`
  display: flex;
  flex-direction: row;
  gap: 10px;
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
  const { total, dateRange, statistics: statisticsList } = statistics || {};
  const missingValues = statisticsList?.reduce(
    (acc, obj) => acc + (obj?.nullValues ?? 0),
    0,
  );

  return (
    <Root>
      <Key>Zeilen:</Key>
      {total}
      <Key>Min Datum:</Key>
      {dateRange?.min}
      <Key>Max Datum:</Key>
      {dateRange?.max}
      <Key>Darumgsbereich:</Key>
      {dateRange?.min && dateRange?.max
        ? `${getDiffInDays(
            parseDate(dateRange?.max, "yyyy-MM-dd") ?? new Date(),
            parseDate(dateRange?.min, "yyyy-MM-dd") ?? new Date(),
          )} ${t("common.timeUnitDays")}`
        : "Datum unbekannt"}
      <Key>Fehlende Werte:</Key>
      {missingValues}
    </Root>
  );
}
