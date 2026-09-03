import {
  faBook,
  faInfoCircle,
  faPaperPlane,
  faQuestion,
} from "@fortawesome/free-solid-svg-icons";
import { MenuTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";

import { useAbout } from "../app/About";
import IconButton from "../button/IconButton";
import FaIcon from "../icon/FaIcon";
import { Menu, MenuItem, menuItemIcon } from "../ui-components/Menu";
import { Popover } from "../ui-components/Popover";

interface Props {
  contactEmail?: string;
  manualUrl?: string;
}

export const HelpMenu = ({ contactEmail, manualUrl }: Props) => {
  const { t } = useTranslation();
  const { setOpen } = useAbout();

  return (
    <MenuTrigger>
      <IconButton
        className="px-3 py-[7px]"
        icon={faQuestion}
        frame
        data-test-id="help-menu"
      />
      <Popover placement="bottom end" offset={5}>
        <Menu
          aria-label={t("common.help")}
          onAction={(key) => {
            if (key === "version") setOpen(true);
          }}
        >
          <MenuItem
            id="contact"
            href={`mailto:${contactEmail}`}
            rel="noopener noreferrer"
            data-test-id="help-email"
          >
            <span className={menuItemIcon()}>
              <FaIcon icon={faPaperPlane} />
            </span>
            {t("common.contact")}
          </MenuItem>
          <MenuItem
            id="manual"
            href={manualUrl}
            target="_blank"
            rel="noopener noreferrer"
            data-test-id="help-manual"
          >
            <span className={menuItemIcon()}>
              <FaIcon icon={faBook} />
            </span>
            {t("common.manual")}
          </MenuItem>
          <MenuItem id="version">
            <span className={menuItemIcon()}>
              <FaIcon icon={faInfoCircle} />
            </span>
            {t("common.version")}
          </MenuItem>
        </Menu>
      </Popover>
    </MenuTrigger>
  );
};
