import {
  DateStatistics,
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";


const DIGITS_OF_PRECISION = 3;
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
  return "mean" in stats;
}

export function previewStatsIsDateStats(
  stats: PreviewStatistics,
): stats is DateStatistics {
  return stats.type === "DATE" || stats.type === "DATE_RANGE";
}
