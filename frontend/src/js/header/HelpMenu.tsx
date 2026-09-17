import {
  BookIcon,
  CircleQuestionMarkIcon,
  InfoIcon,
  SendIcon,
} from "lucide-react";
import { MenuTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { useAbout } from "../app/About";
import { Button } from "../ui-components/Button";
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
      <Button
        intent="secondary"
        aria-label={t("common.help")}
        data-test-id="help-menu"
      >
        <Icon icon={CircleQuestionMarkIcon} />
      </Button>
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
          <Icon icon={SendIcon} />
          {t("common.contact")}
        </MenuItem>
        <MenuItem
          id="manual"
          href={manualUrl}
          target="_blank"
          rel="noopener noreferrer"
          data-test-id="help-manual"
        >
          <Icon icon={BookIcon} />
          {t("common.manual")}
        </MenuItem>
        <MenuItem id="version">
          <Icon icon={InfoIcon} />
          {t("common.version")}
        </MenuItem>
      </Menu>
    </MenuTrigger>
  );
};
