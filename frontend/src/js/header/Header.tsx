import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import styled from "@emotion/styled";
import { useSelector } from "react-redux";
import { StateT } from "app-types";

import { useHideLogoutButton } from "../user/selectors";
import DatasetSelector from "../dataset/DatasetSelector";
import LogoutButton from "./LogoutButton";

const Root = styled("header")`
  background-color: ${({ theme }) => theme.col.graySuperLight};
  color: ${({ theme }) => theme.col.blueGrayDark};
  border-bottom: 1px solid ${({ theme }) => theme.col.grayMediumLight};
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.3);
  padding: 0 20px;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;

  // Fix, so content can expand to 100% and scroll
  position: absolute;
  z-index: 3;
  width: 100%;
  top: 0;
  left: 0;
`;

const Right = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;

const OverflowHidden = styled("div")`
  overflow: hidden;
  flex-shrink: 0;
  display: flex;
  flex-direction: row;
  align-items: center;
`;

const Spacer = styled("span")`
  margin: 0 10px;
  height: 20px;
`;

const Logo = styled("div")`
  height: 50px;
  width: ${({ theme }) => theme.img.logoWidth};
  background-image: url(${({ theme }) => theme.img.logo});
  background-repeat: no-repeat;
  background-position-y: 50%;
  background-size: ${({ theme }) => theme.img.logoBackgroundSize};
`;

const Headline = styled("h1")`
  margin: 0 auto 0 0;
  line-height: 2;
  font-size: ${({ theme }) => theme.font.md};
  font-weight: 300;
`;

const SxLogoutButton = styled(LogoutButton)`
  margin-left: 5px;
`;

const Header: FC = () => {
  const { t } = useTranslation();
  const version = useSelector<StateT, string>(
    (state) => state.startup.config.version
  );

  const hideLogoutButton = useHideLogoutButton();

  return (
    <Root>
      <OverflowHidden>
        <Logo title={version} />
        <Spacer />
        <Headline>{t("headline")}</Headline>
      </OverflowHidden>
      <Right>
        <DatasetSelector />
        {!hideLogoutButton && <SxLogoutButton />}
      </Right>
    </Root>
  );
};

export default Header;
