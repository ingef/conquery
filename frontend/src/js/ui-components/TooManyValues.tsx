import { XIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Button } from "./Button";
import { C2 } from "./Typography";

const root = tv({
  base: [
    "flex flex-row items-center",
    "px-[10px] py-[3px]",
    "border border-gray-500",
    "rounded",
  ],
});

const TooManyValues = ({
  count,
  onClear,
}: {
  count: number;
  onClear: () => void;
}) => {
  const { t } = useTranslation();

  return (
    <div className={root()}>
      <C2>{t("queryNodeEditor.tooManyValues", { count })}</C2>
      <Button
        intent="tertiary"
        size="sm"
        aria-label={t("common.clearValue")}
        onPress={onClear}
      >
        <XIcon />
      </Button>
    </div>
  );
};

export default TooManyValues;
