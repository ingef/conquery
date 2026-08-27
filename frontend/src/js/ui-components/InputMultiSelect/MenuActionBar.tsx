import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { TransparentButton } from "../../button/TransparentButton";
import { exists } from "../../common/helpers/exists";

const row = tv({
  base: [
    "flex items-center justify-between",
    "px-[10px] py-[5px]",
    "border-b border-[#ccc]",
  ],
});

const infoText = tv({
  base: ["m-0 mr-[10px]", "text-gray-500", "text-xs"],
});

interface Props {
  optionsCount: number;
  total?: number;
  onInsertAllClick: () => void;
}

const MenuActionBar = ({ optionsCount, total, onInsertAllClick }: Props) => {
  const { t } = useTranslation();

  return (
    <div className={row()}>
      <p className={infoText()}>
        {t("inputMultiSelect.options", { count: optionsCount })}
        {exists(total) &&
          total !== optionsCount &&
          t("inputMultiSelect.ofTotal", { count: total })}
      </p>
      <TransparentButton
        tiny
        disabled={optionsCount === 0}
        onClick={onInsertAllClick}
      >
        {t("inputMultiSelect.insertAll")}
      </TransparentButton>
    </div>
  );
};

export default MenuActionBar;
