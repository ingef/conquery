import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";

const root = tv({
  base: [
    "flex flex-row items-center",
    "px-[10px] py-[3px]",
    "border border-gray-500",
    "rounded",
  ],
});

const text = tv({ base: ["m-0", "leading-none"] });

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
      <p className={text()}>{t("queryNodeEditor.tooManyValues", { count })}</p>
      <IconButton
        icon={faTimes}
        tiny
        aria-label={t("common.clearValue")}
        onClick={onClear}
      />
    </div>
  );
};

export default TooManyValues;
