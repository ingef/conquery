import { useNumberFormatter } from "react-aria";

import type { CurrencyConfigT } from "../../api/types";

// money in the app's locale with the configured decimals, like NumberField
export const useMoneyFormatter = ({ decimalScale }: CurrencyConfigT) =>
  useNumberFormatter({
    minimumFractionDigits: decimalScale,
    maximumFractionDigits: decimalScale,
  });
