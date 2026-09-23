import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { exists } from "../../common/helpers/exists";
import { Button } from "../Button";
import { C3 } from "../Typography";

const row = tv({
  base: [
    "flex items-center justify-between",
    "px-[10px] py-[5px]",
    "border-b border-[#ccc]",
  ],
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
      <C3 tone="muted">
        {t("inputMultiSelect.options", { count: optionsCount })}
        {exists(total) &&
          total !== optionsCount &&
          t("inputMultiSelect.ofTotal", { count: total })}
      </C3>
      <Button
        intent="secondary"
        size="sm"
        isDisabled={optionsCount === 0}
        onPress={onInsertAllClick}
      >
        {t("inputMultiSelect.insertAll")}
      </Button>
    </div>
  );
};

export default MenuActionBar;
