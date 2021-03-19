import i18next from "i18next";
import { initReactI18next } from "react-i18next";
import LanguageDetector from "i18next-browser-languagedetector";

i18next
  .use(initReactI18next)
  .use(LanguageDetector)
  .init({
    lng: "de",
    fallbackLng: "de",
    debug: true,
    resources: {
      de: { translation: {} },
      en: { translation: {} },
    },
  });

export default i18next;
