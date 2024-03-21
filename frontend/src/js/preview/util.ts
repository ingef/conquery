import { BarStatistics, DateStatistics, PreviewStatistics } from "../api/types";

export const NUMBER_TYPES = ["NUMERIC", "INTEGER"];

export const NUMBER_STATISTICS_TYPES = [...NUMBER_TYPES, "MONEY"];

export function currencyFromSymbol(symbol: string): string {
  //TODO this is a workaround until the backend sends currency-codes
  if (symbol == "€") {
    return "EUR";
  }

  if (symbol == "$") {
    return "USD";
  }

  return "USD";
}

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
