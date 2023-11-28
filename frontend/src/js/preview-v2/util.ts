export function formatNumber(num: number): string {
  return num.toPrecision(3).toLocaleString().replace(".", ",");
}