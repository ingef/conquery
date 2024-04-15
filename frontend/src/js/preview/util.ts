import { t } from "i18next";
import { BarStatistics, DateStatistics, PreviewStatistics } from "../api/types";
import { parseDate } from "../common/helpers/dateHelper";

export const NUMBER_TYPES = ["NUMERIC", "INTEGER"];

export const NUMBER_STATISTICS_TYPES = [...NUMBER_TYPES, "MONEY"];

export function formatNumber(
  num: number,
  {
    precision = 2,
    forceFractionDigits,
  }: { precision?: number; forceFractionDigits?: boolean } = {},
): string {
  return new Intl.NumberFormat(navigator.language, {
    maximumFractionDigits: precision,
    minimumFractionDigits: forceFractionDigits ? precision : undefined,
  }).format(num);
}

export function formatDate(date: string | undefined) {
  if (date) {
    const parsedDate = parseDate(date, "yyyy-MM-dd");
    return parsedDate ? toFullLocaleDateString(parsedDate) : date;
  }
  return t("preview.dateError");
}

export function toFullLocaleDateString(date: Date) {
  return date.toLocaleDateString(navigator.language, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  });
}

export function previewStatsIsBarStats(
  stats: PreviewStatistics,
): stats is BarStatistics {
  return stats.chart === "HISTO";
}

export function previewStatsIsDateStats(
  stats: PreviewStatistics,
): stats is DateStatistics {
  return stats.chart === "DATES";
}

export function useDateTickHandler(stats: PreviewStatistics) {
  let shouldTickRender: (date: Date) => boolean = () => false;

  if (previewStatsIsDateStats(stats)) {
    const quarterCount = Object.keys(stats.quarterCounts).length;

    // default -> months
    shouldTickRender = () => true;
    // 2 years -> quarters
    if (quarterCount >= 8) {
      shouldTickRender = (date: Date) => date.getMonth() % 3 === 0;
    }
    // 4 years -> halfyears
    if (quarterCount >= 16) {
      shouldTickRender = (date: Date) => date.getMonth() % 6 === 0;
    }
    // 8 years -> years
    if (quarterCount >= 32) {
      shouldTickRender = (date: Date) => date.getMonth() === 0;
    }
    // 10 years -> decades
    if (quarterCount >= 160) {
      shouldTickRender = (date: Date) =>
        date.getFullYear() % 10 === 0 && date.getMonth() === 0;
    }
  }

  return { shouldTickRender };
}
