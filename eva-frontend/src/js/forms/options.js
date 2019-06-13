// @flow

export const REGIONS = [
  { value: "DEUTSCHLAND", label: "Deutschland" },
  { value: "SCHLESWIG_HOLSTEIN", label: "Schleswig-Holstein" },
  { value: "HAMBURG", label: "Hamburg" },
  { value: "NIEDERSACHSEN", label: "Niedersachsen" },
  { value: "BREMEN", label: "Bremen" },
  { value: "NORDRHEIN_WESTFALEN", label: "Nordrhein-Westfalen" },
  { value: "HESSEN", label: "Hessen" },
  { value: "RHEINLAND_PFALZ", label: "Rheinland-Pfalz" },
  { value: "BADEN_WUERTTEMBERG", label: "Baden-Württemberg" },
  { value: "BAYERN", label: "Bayern" },
  { value: "SAARLAND", label: "Saarland" },
  { value: "BERLIN", label: "Berlin" },
  { value: "BRANDENBURG", label: "Brandenburg" },
  { value: "MECKLENBURG_VORPOMMERN", label: "Mecklenburg-Vorpommern" },
  { value: "SACHSEN", label: "Sachsen" },
  { value: "SACHSEN_ANHALT", label: "Sachsen-Anhalt" },
  { value: "THUERINGEN", label: "Thüringen" }
];

export const GRANULARITY_LEVELS = [
  "FEDERAL_STATE",
  "DISTRICT",
  "ZIP_CODE",
  "KV"
];

export const RESOLUTION_OPTIONS = [
  "COMPLETE_ONLY",
  "QUARTER_WISE",
  "YEAR_WISE"
];

export const INDEX_DATE_OPTIONS = [
  "BEFORE",
  "NEUTRAL",
  "AFTER"
];
