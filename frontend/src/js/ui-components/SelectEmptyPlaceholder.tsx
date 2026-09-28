import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { C2 } from "./Typography";

const container = tv({
  base: ["flex items-center justify-center", "w-full", "py-[6px]"],
});

const SelectEmptyPlaceholder = () => {
  const { t } = useTranslation();
  return (
    <div className={container()}>
      <C2 tone="muted">{t("inputSelect.empty")}</C2>
    </div>
  );
};

export default SelectEmptyPlaceholder;
