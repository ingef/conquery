import {
  DateStatistics,
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";
import { numberToThreeDigitArray } from "../common/helpers/commonHelper";

const NUMBER_STATISTICS_TYPES = [
  "NUMBER",
  "INTEGER",
  "REAL",
  "DECIMAL",
  "MONEY",
];

const DIGITS_OF_PRECISION = 2;
export function formatNumber(num: number): string {
  return num.toLocaleString();
  // TODO verify localeString implementation
  if (num > 100) {
    return numberToThreeDigitArray(Math.floor(num)).join(".")
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
