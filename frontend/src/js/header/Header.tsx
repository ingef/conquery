import styled from "@emotion/styled";
import preval from "preval.macro";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import DatasetSelector from "../dataset/DatasetSelector";
import { useHideLogoutButton } from "../user/selectors";

import { LinkToManual } from "./LinkToManual";
import LogoutButton from "./LogoutButton";

const Root = styled("header")`
  background-color: ${({ theme }) => theme.col.bg};
  box-shadow: 0 0 1px 1px rgba(0, 0, 0, 0.3);
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
  gap: 5px;
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
  const frontendVersion = `${frontendDateTimeStamp}`;

  return {
    backendVersion,
    frontendVersion,
  };
};

const Header: FC = () => {
  const { t } = useTranslation();
  const { backendVersion, frontendVersion } = useVersion();
  const hideLogoutButton = useHideLogoutButton();
  const { manualUrl, contactEmail } = useSelector<
    StateT,
    StateT["startup"]["config"]
  >((state) => state.startup.config);

  const versionString = `BE: ${backendVersion}, FE: ${frontendVersion}`;

  const copyVersionToClipboard = () => {
    navigator.clipboard.writeText(
      `BE: ${backendVersion} FE: ${frontendVersion}`,
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
        {(manualUrl || contactEmail) && (
          <LinkToManual manualUrl={manualUrl} contactEmail={contactEmail} />
        )}
        {!hideLogoutButton && <LogoutButton />}
      </Right>
    </Root>
  );
};

export default Header;
