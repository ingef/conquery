import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import type { ReactNode } from "react";
import { MenuTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";

import { Icon } from "./Icon";

import { Menu, MenuItem } from "./Menu";

/**
 * Asks for confirmation before calling onConfirm: a menu with a single item.
 * The child is the trigger, a button built on BasicButton.
 */
export const ConfirmMenu = ({
  children,
  confirmationIcon,
  confirmationText,
  placement,
  onConfirm,
  red,
}: {
  children: ReactNode;
  confirmationText?: string;
  confirmationIcon?: IconProp;
  placement?: "top" | "bottom" | "left" | "right";
  onConfirm: () => void;
  red?: boolean;
}) => {
  const { t } = useTranslation();
  const label = confirmationText || t("common.confirm");

  return (
    <MenuTrigger>
      {children}
      <Menu aria-label={label} placement={placement} onAction={onConfirm}>
        <MenuItem id="confirm" danger={red} data-test-id="confirm">
          <Icon icon={confirmationIcon || faCheck} />
          {label}
        </MenuItem>
      </Menu>
    </MenuTrigger>
  );
};
