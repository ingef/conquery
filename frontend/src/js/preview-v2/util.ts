import { t } from "i18next";
import { BarStatistics, DateStatistics, PreviewStatistics } from "../api/types";
import { parseDate } from "../common/helpers/dateHelper";

export const NUMBER_TYPES = ["NUMERIC", "INTEGER"];

export const NUMBER_STATISTICS_TYPES = [...NUMBER_TYPES, "MONEY"];

export function formatNumber(num: number, precision = 2): string {
  return new Intl.NumberFormat(navigator.language, {
    maximumFractionDigits: precision,
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
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
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
