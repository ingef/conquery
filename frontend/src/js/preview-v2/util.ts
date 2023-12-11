import {
  DateStatistics,
  NumberStatistics,
  PreviewStatistics,
  StringStatistics,
} from "../api/types";
import { numberToThreeDigitArray } from "../common/helpers/commonHelper";


const DIGITS_OF_PRECISION = 3;
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
  return "mean" in stats;
}

export function previewStatsIsDateStats(
  stats: PreviewStatistics,
): stats is DateStatistics {
  return stats.type === "DATE" || stats.type === "DATE_RANGE";
}
