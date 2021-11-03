import styled from "@emotion/styled";
import { StateT } from "app-types";
import preval from "preval.macro";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import DatasetSelector from "../dataset/DatasetSelector";
import { useHideLogoutButton } from "../user/selectors";

import LogoutButton from "./LogoutButton";

const Root = styled("header")`
  background-color: ${({ theme }) => theme.col.bg};
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
  margin: 0 5px;
  height: 20px;
`;

const Logo = styled("div")`
  height: 40px;
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
  font-weight: 700;
  font-size: 12px;
  opacity: 0.3;
  text-transform: uppercase;
  color: ${({ theme }) => theme.col.blueGrayDark};
`;

const SxLogoutButton = styled(LogoutButton)`
  margin-left: 5px;
`;

const useVersion = () => {
  const backendVersion = useSelector<StateT, string>(
    (state) => state.startup.config.version,
  );

  const frontendDateTimeStamp = preval`module.exports = new Date().toISOString();`;
  // TODO: GET THIS TO WORK WHEN BUILDING INSIDE A DODCKER CONTAINER
  // const frontendGitCommit = preval`
  //   const { execSync } = require('child_process');
  //   module.exports = execSync('git rev-parse --short HEAD').toString();
  // `;
  // const frontendGitTag = preval`
  //   const { execSync } = require('child_process');
  //   module.exports = execSync('git describe --all --exact-match \`git rev-parse HEAD\`').toString();
  // `;

  return {
    backendVersion,
    frontendGitCommit: "",
    frontendDateTimeStamp,
    frontendGitTag: "",
  };
};

const Header: FC = () => {
  const { t } = useTranslation();
  const {
    backendVersion,
    frontendDateTimeStamp,
    frontendGitTag,
    frontendGitCommit,
  } = useVersion();
  const hideLogoutButton = useHideLogoutButton();

  const versionString = `BE: ${backendVersion}, FE: ${frontendGitTag} ${frontendGitCommit} ${frontendDateTimeStamp}`;

  const copyVersionToClipboard = () => {
    navigator.clipboard.writeText(
      `${backendVersion} ${frontendGitTag} ${frontendGitCommit}`,
    );
  };

  return (
    <Root>
      <OverflowHidden>
        <Logo title={versionString} onClick={copyVersionToClipboard} />
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
