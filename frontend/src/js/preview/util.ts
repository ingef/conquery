import { BarStatistics, DateStatistics, PreviewStatistics } from "../api/types";

export const NUMBER_TYPES = ["NUMERIC", "INTEGER"];

export const NUMBER_STATISTICS_TYPES = [...NUMBER_TYPES, "MONEY"];

export function currencyFromSymbol(symbol: string): string {
  // TODO: this is a workaround until the backend sends currency-codes
  if (symbol === "€") return "EUR";

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

export function useDateTickHandler(stats: PreviewStatistics) {
  let shouldTickRender: (date: Date) => boolean = () => false;

  if (previewStatsIsDateStats(stats)) {
    const quarterCount = Object.keys(stats.quarterCounts).length;

    // default -> months
    shouldTickRender = () => true;
    // > 12 months -> quarters
    if (quarterCount > 4) {
      shouldTickRender = (date: Date) => date.getMonth() % 3 === 0;
    }
    // > 12 quarters -> halfyears
    if (quarterCount > 12) {
      shouldTickRender = (date: Date) => date.getMonth() % 6 === 0;
    }
    // > 12 halfyears -> years
    if (quarterCount > 24) {
      shouldTickRender = (date: Date) => date.getMonth() === 0;
    }
    // > 12 years -> decades
    if (quarterCount > 48) {
      shouldTickRender = (date: Date) =>
        date.getFullYear() % 10 === 0 && date.getMonth() === 0;
    }
  }

  return { shouldTickRender };
}
