import styled from "@emotion/styled";
import { faAngleLeft } from "@fortawesome/free-solid-svg-icons";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";

import tw from "tailwind-styled-components";
import { toggleDisplayTooltip } from "./actions";

const Header = tw("h2")`
  bg-white
  h-[40px]
  flex-shrink-0
  flex items-center
  px-5
  pt-1
  text-sm
  tracking-[1px]
  uppercase
  text-primary-500
  border-b border-gray-100
  font-bold
`;

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
      <Header>{t("tooltip.headline")}</Header>
    </>
  );
});
