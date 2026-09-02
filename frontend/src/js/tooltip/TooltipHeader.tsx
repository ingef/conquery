import styled from "@emotion/styled";
import { faAngleLeft } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { tv } from "tailwind-variants";
import IconButton from "../button/IconButton";
import { toggleDisplayTooltip } from "./actions";

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

const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 40px;
  height: 39px;
  right: 0;
  border-radius: 0;
`;

export const TooltipHeader = memo(() => {
  const { t } = useTranslation();

  const dispatch = useDispatch();
  const onToggleDisplayTooltip = () => dispatch(toggleDisplayTooltip());

  return (
    <>
      <StyledIconButton
        bgHover
        onClick={onToggleDisplayTooltip}
        icon={faAngleLeft}
      />
      <h2 className={header()}>{t("tooltip.headline")}</h2>
    </>
  );
});
