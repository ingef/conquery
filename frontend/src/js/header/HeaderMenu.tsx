import {
  BookIcon,
  InfoIcon,
  ListIcon,
  LogOutIcon,
  MenuIcon,
  SendIcon,
} from "lucide-react";
import { MenuTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { useAbout } from "../app/About";
import type { StateT } from "../app/reducers";
import { openHistory } from "../entity-history/actions";
import { Button } from "../ui-components/Button";
import { Menu, MenuItem, MenuSeparator } from "../ui-components/Menu";
import { canViewEntityPreview, useHideLogoutButton } from "../user/selectors";
import { useLogout } from "./useLogout";

interface Props {
  contactEmail?: string;
  manualUrl?: string;
}

export const HeaderMenu = ({ contactEmail, manualUrl }: Props) => {
  const { t } = useTranslation();
  const { setOpen } = useAbout();
  const logout = useLogout();
  const hideLogout = useHideLogoutButton();
  const canViewHistory = useSelector<StateT, boolean>(canViewEntityPreview);
  const dispatch = useDispatch();

  return (
    <MenuTrigger>
      <Button
        intent="secondary"
        aria-label={t("common.menu")}
        data-test-id="header-menu"
      >
        <MenuIcon />
      </Button>
      <Menu
        aria-label={t("common.menu")}
        placement="bottom end"
        onAction={(key) => {
          if (key === "history") dispatch(openHistory());
          if (key === "version") setOpen(true);
          if (key === "logout") logout();
        }}
      >
        {canViewHistory && (
          <MenuItem id="history">
            <ListIcon />
            {t("history.history")}
          </MenuItem>
        )}
        {canViewHistory && <MenuSeparator />}
        {contactEmail && (
          <MenuItem
            id="contact"
            href={`mailto:${contactEmail}`}
            rel="noopener noreferrer"
            data-test-id="help-email"
          >
            <SendIcon />
            {t("common.contact")}
          </MenuItem>
        )}
        {manualUrl && (
          <MenuItem
            id="manual"
            href={manualUrl}
            target="_blank"
            rel="noopener noreferrer"
            data-test-id="help-manual"
          >
            <BookIcon />
            {t("common.manual")}
          </MenuItem>
        )}
        <MenuItem id="version">
          <InfoIcon />
          {t("common.version")}
        </MenuItem>
        {!hideLogout && <MenuSeparator />}
        {!hideLogout && (
          <MenuItem id="logout" data-test-id="logout">
            <LogOutIcon />
            {t("common.logout")}
          </MenuItem>
        )}
      </Menu>
    </MenuTrigger>
  );
};
