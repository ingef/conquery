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
import { Icon } from "../ui-components/Icon";
import { Menu, MenuItem } from "../ui-components/Menu";

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
        className="p-[7px]"
        icon={faQuestion}
        frame
        data-test-id="help-menu"
      />
      <Menu
        aria-label={t("common.help")}
        placement="bottom end"
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
          <Icon icon={faPaperPlane} />
          {t("common.contact")}
        </MenuItem>
        <MenuItem
          id="manual"
          href={manualUrl}
          target="_blank"
          rel="noopener noreferrer"
          data-test-id="help-manual"
        >
          <Icon icon={faBook} />
          {t("common.manual")}
        </MenuItem>
        <MenuItem id="version">
          <Icon icon={faInfoCircle} />
          {t("common.version")}
        </MenuItem>
      </Menu>
    </MenuTrigger>
  );
};
