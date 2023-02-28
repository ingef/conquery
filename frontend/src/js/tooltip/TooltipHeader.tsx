import styled from "@emotion/styled";
import { memo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";

import { toggleDisplayTooltip } from "./actions";

const Header = styled("h2")`
  background-color: white;
  height: 40px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  margin: 0;
  padding: 0 20px;
  font-size: ${({ theme }) => theme.font.sm};
  letter-spacing: 1px;
  line-height: 38px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.blueGrayDark};
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
        icon="angle-left"
      />
      <Header>{t("tooltip.headline")}</Header>
    </>
  );
});
