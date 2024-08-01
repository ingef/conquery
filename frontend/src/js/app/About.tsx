import { faCopy } from "@fortawesome/free-regular-svg-icons";
import {
  Fragment,
  ReactNode,
  createContext,
  memo,
  useCallback,
  useContext,
  useState,
} from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useSelector } from "react-redux";

import IconButton from "../button/IconButton";
import Modal from "../modal/Modal";

import { GetFrontendConfigResponseT } from "../api/types";
import { StateT } from "./reducers";

const initialState = {
  isOpen: false,
  setOpen: () => {},
};

const Context = createContext<ReturnType<typeof useContextValue>>(initialState);

const useContextValue = () => {
  const [isOpen, setOpen] = useState(false);

  return { isOpen, setOpen };
};

export const AboutProvider = ({ children }: { children: ReactNode }) => (
  <Context.Provider value={useContextValue()}>{children}</Context.Provider>
);

export const useAbout = () => {
  return useContext(Context);
};

const useVersion = () => {
  const backendVersions = useSelector<
    StateT,
    GetFrontendConfigResponseT["versions"]
  >((state) => state.startup.config.versions);

  // THIS IS GETTING STATICALLY REPLACED USING "VITE DEFINE"
  const frontendTimestamp = `__BUILD_TIMESTAMP__`.replace(/"/g, "");
  const frontendGitDescribe = `__BUILD_GIT_DESCRIBE__`.replace(/"/g, "");

  return {
    backendVersions,
    frontendTimestamp,
    frontendGitDescribe,
  };
};

export const About = memo(() => {
  const { isOpen, setOpen } = useAbout();
  const toggleOpen = useCallback(() => setOpen((open) => !open), [setOpen]);
  const { backendVersions, frontendTimestamp, frontendGitDescribe } =
    useVersion();

  const copyVersionToClipboard = () => {
    navigator.clipboard.writeText(
      `${backendVersions
        .map(({ name, version }) => `${name}: ${version}`)
        .join(" ")} Frontend: ${frontendGitDescribe}`,
    );
    setOpen(false);
  };

  useHotkeys("shift+?", toggleOpen, [toggleOpen]);

  if (!isOpen) return null;

  return (
    <Modal headline="Version" onClose={() => setOpen(false)}>
      <div className="space-y-5">
        <div className="grid grid-cols-[auto_1fr] gap-x-5 gap-y-1">
          {backendVersions.map((version) => (
            <Fragment key={version.name}>
              <div>{version.name}</div>
              <code className="font-bold">
                {version.version || "-"}
                {version.buildTime && ` – ${version.buildTime}`}
              </code>
            </Fragment>
          ))}
          <div>Frontend</div>
          <code className="font-bold">
            {frontendGitDescribe} – {frontendTimestamp}
          </code>
        </div>
        <IconButton frame icon={faCopy} onClick={copyVersionToClipboard}>
          Copy version info
        </IconButton>
      </div>
    </Modal>
  );
});
