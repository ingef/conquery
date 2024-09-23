import { useTranslation } from "react-i18next";

export type Language = "de" | "en";

export const useActiveLang = () => {
  const { i18n } = useTranslation();

  return i18n.language as unknown as Language;
};
