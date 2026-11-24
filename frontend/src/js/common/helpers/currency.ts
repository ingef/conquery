// what Intl knows about a currency, for fields that show money as plain decimals
export const currencyFractionDigits = (currency: string) =>
  new Intl.NumberFormat("en", { style: "currency", currency }).resolvedOptions()
    .maximumFractionDigits ?? 2;

export const currencySymbol = (locale: string, currency: string) =>
  new Intl.NumberFormat(locale, { style: "currency", currency })
    .formatToParts(0)
    .find((part) => part.type === "currency")?.value ?? currency;
