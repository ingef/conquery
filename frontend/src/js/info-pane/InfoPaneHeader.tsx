import { faAngleLeft } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import { Icon } from "../ui-components/Icon";
import { toggleInfoPane } from "./actions";

const header = tv({
  base: [
    "flex items-center",
    "h-[40px]",
    "shrink-0",
    "border-b border-gray-100",
    "bg-white",
    "px-5 pt-1",
    "text-sm",
    "font-bold",
    "uppercase",
    "tracking-[1px]",
    "text-primary-500",
  ],
});

// as tall as the header it sits in
const toggleButton = tv({
  base: [
    "absolute top-[40px] right-0",
    "h-[39px] w-[30px]",
    "flex items-center justify-center",
    "text-gray-800",
    "cursor-pointer",
    "hover:bg-gray-50",
  ],
});

export const InfoPaneHeader = memo(() => {
  const { t } = useTranslation();

  const dispatch = useDispatch();
  const onToggleInfoPane = () => dispatch(toggleInfoPane());

  return (
    <>
      <RacButton className={toggleButton()} onPress={onToggleInfoPane}>
        <Icon icon={faAngleLeft} />
      </RacButton>
      <h2 className={header()}>{t("infoPane.headline")}</h2>
    </>
  );
});
