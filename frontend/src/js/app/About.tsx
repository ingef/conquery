import styled from "@emotion/styled";
import { faCopy } from "@fortawesome/free-regular-svg-icons";
import { memo, useState } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useSelector } from "react-redux";

import IconButton from "../button/IconButton";
import Modal from "../modal/Modal";

import { StateT } from "./reducers";

const Grid = styled("div")`
  display: grid;
  grid-template-columns: auto 1fr;
  margin-bottom: 20px;
  gap: 5px 20px;
`;

const Version = styled("code")`
  font-size: 16px;
  font-weight: bold;
`;

const useVersion = () => {
  const backendVersion = useSelector<StateT, string>(
    (state) => state.startup.config.version,
  );

  // TODO: GET THIS TO WORK WHEN BUILDING INSIDE A DOCKER CONTAINER
  // const frontendGitCommit = preval`
  //   const { execSync } = require('child_process');
  //   module.exports = execSync('git rev-parse --short HEAD').toString();
  // `;
  // const frontendGitTag = preval`
  //   const { execSync } = require('child_process');
  //   module.exports = execSync('git describe --all --exact-match \`git rev-parse HEAD\`').toString();
  // `;

  // THIS IS GETTING STATICALLY REPLACED USING "VITE DEFINE"
  const frontendVersion = `__BUILD_TIMESTAMP__`.replace(/"/g, "");

  return {
    backendVersion,
    frontendVersion,
  };
};

export const About = memo(() => {
  const [isOpen, setIsOpen] = useState(false);
  const { backendVersion, frontendVersion } = useVersion();

  const copyVersionToClipboard = () => {
    navigator.clipboard.writeText(
      `BE: ${backendVersion} FE: ${frontendVersion}`,
    );
    setIsOpen(false);
  };

  useHotkeys("shift+?", () => setIsOpen((open) => !open));

  if (!isOpen) return null;

  return (
    <Modal headline="Version" onClose={() => setIsOpen(false)}>
      <Grid>
        <div>Backend</div>
        <Version>{backendVersion}</Version>
        <div>Frontend</div>
        <Version>{frontendVersion}</Version>
      </Grid>
      <IconButton frame icon={faCopy} onClick={copyVersionToClipboard}>
        Copy version info
      </IconButton>
    </Modal>
  );
});
