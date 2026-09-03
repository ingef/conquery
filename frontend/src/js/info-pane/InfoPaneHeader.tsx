import { faAngleLeft } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import { Button } from "../ui-components/Button";
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

const toggleButton = tv({
  base: ["absolute top-[40px] right-0", "h-[39px]", "rounded-none"],
});

export const InfoPaneHeader = memo(() => {
  const { t } = useTranslation();

  const dispatch = useDispatch();
  const onToggleInfoPane = () => dispatch(toggleInfoPane());

  return (
    <>
      <Button
        intent="tertiary"
        onPress={onToggleInfoPane}
        className={toggleButton()}
      >
        <Icon icon={faAngleLeft} />
      </Button>
      <h2 className={header()}>{t("infoPane.headline")}</h2>
    </>
  );
});
