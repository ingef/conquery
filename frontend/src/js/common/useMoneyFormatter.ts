import { useNumberFormatter } from "react-aria";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";

/** the ISO 4217 code of the currency that money columns hold */
export const useCurrency = () =>
  useSelector<StateT, string>((state) => state.startup.config.currency);

/** money in the app's locale, with the currency's symbol and decimals */
export const useMoneyFormatter = () => {
  const currency = useCurrency();

  return useNumberFormatter({ style: "currency", currency });
};
