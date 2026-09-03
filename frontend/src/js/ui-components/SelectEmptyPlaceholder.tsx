import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

const container = tv({
  base: [
    "flex items-center justify-center",
    "w-full",
    "py-[6px]",
    "text-sm",
    "text-gray-500",
  ],
});

const SelectEmptyPlaceholder = () => {
  const { t } = useTranslation();
  return <div className={container()}>{t("inputSelect.empty")}</div>;
};

export default SelectEmptyPlaceholder;
