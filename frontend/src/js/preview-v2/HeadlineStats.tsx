import styled from "@emotion/styled";
import { DateRangeT } from "../api/types";
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

interface HeadlineStatsLoaded {
  numberOfRows: number;
  dateRange: DateRangeT;
  missingValues: number;
  loading: false;
}

interface HeadlineStatsLoading {
  loading: true;
}

export default function HeadlineStats(
  props: HeadlineStatsLoaded | HeadlineStatsLoading,
) {
  if (props.loading) {
    return (
      <Root>
        <Key>Zeilen:</Key>
        <Key>Min Datum:</Key>
        <Key>Max Datum:</Key>
        <Key>Darumgsbereich:</Key>
        <Key>Fehlende Werte:</Key>
      </Root>
    );
  }

  const { numberOfRows, dateRange, missingValues } =
    props as HeadlineStatsLoaded;

  return (
    <Root>
      <Key>Zeilen:</Key>
      {numberOfRows}
      <Key>Min Datum:</Key>
      {dateRange.min}
      <Key>Max Datum:</Key>
      {dateRange.max}
      <Key>Darumgsbereich:</Key>
      {dateRange.min && dateRange.max
        ? `${getDiffInDays(
            parseDate(dateRange.max, "yyyy-MM-dd") ?? new Date(),
            parseDate(dateRange.min, "yyyy-MM-dd") ?? new Date(),
        )} ${t("common.timeUnitDays")}`
        : "Datum unbekannt"}
      <Key>Fehlende Werte:</Key>
      {missingValues}
    </Root>
  );
}
