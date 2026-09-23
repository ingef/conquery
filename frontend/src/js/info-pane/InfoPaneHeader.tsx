import { ChevronLeftIcon } from "lucide-react";
import { memo } from "react";
import { Button as RacButton } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import { H3 } from "../ui-components/Typography";
import { toggleInfoPane } from "./actions";

const header = tv({
  base: [
    "flex items-center",
    "h-pane-header",
    "shrink-0",
    "border-b border-gray-100",
    "bg-white",
    "px-5",
  ],
});

// as tall as the header it sits in, above the header's line
const toggleButton = tv({
  base: [
    "absolute top-header right-0",
    "h-[37px] w-[30px]",
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
        <ChevronLeftIcon />
      </RacButton>
      <div className={header()}>
        <H3 as="h2">{t("infoPane.headline")}</H3>
      </div>
    </>
  );
});
