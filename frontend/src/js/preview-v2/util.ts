import { t } from "i18next";
import {
  DateStatistics,
  PreviewStatistics,
  BarStatistics,
} from "../api/types";
import { parseDate } from "../common/helpers/dateHelper";

export const NUMBER_TYPES = [
  "NUMBER",
  "INTEGER",
  "REAL",
  "DECIMAL",
];

export const NUMBER_STATISTICS_TYPES = [
  ...NUMBER_TYPES,
  "MONEY",
];

export function formatNumber(num: number, precision = 3): string {
  const precisionMultiplier = 10 * precision;
  return (
    Math.round(num * precisionMultiplier) / precisionMultiplier
  ).toLocaleString("de-de");
}

export function formatDate(date: string | undefined) {
  if (date) {
    return (
      parseDate(date, "yyyy-MM-dd")?.toLocaleDateString("de-de") ??
      t("preview.dateError")
    );
  }
  return t("preview.dateError");
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
