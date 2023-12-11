import {
  DateStatistics,
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";

const NUMBER_STATISTICS_TYPES = [
  "NUMBER",
  "INTEGER",
  "REAL",
  "DECIMAL",
  "MONEY",
];

const DIGITS_OF_PRECISION = 2;
export function formatNumber(num: number): string {
  if (num > 100) {
    return num.toFixed(0).replace(".", ",");
  }

  return num
    .toPrecision(DIGITS_OF_PRECISION)
    .toLocaleString()
    .replace(".", ",");
}

export function previewStatsIsStringStats(
  stats: PreviewStatistics,
): stats is StringStatistics {
  return stats.type === "STRING";
}

export function previewStatsIsNumberStats(
  stats: PreviewStatistics,
): stats is NumberStatistics {
  return NUMBER_STATISTICS_TYPES.indexOf(stats.type) !== -1; // && "stddev" in stats;
}

export function previewStatsIsDateStats(
  stats: PreviewStatistics,
): stats is DateStatistics {
  return stats.type === "DATE" || stats.type === "DATE_RANGE";
}
