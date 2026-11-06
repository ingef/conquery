import { CheckIcon, type LucideIcon } from "lucide-react";
import type { ReactNode } from "react";
import { MenuTrigger } from "react-aria-components";
import { useTranslation } from "react-i18next";

import { Menu, MenuItem } from "./Menu";

/**
 * Asks for confirmation before calling onConfirm: a menu with a single item.
 * The child is the trigger: a Button or ToggleButton.
 */
export const ConfirmMenu = ({
  children,
  confirmationIcon: ConfirmationIcon = CheckIcon,
  confirmationText,
  placement,
  onConfirm,
  red,
}: {
  children: ReactNode;
  confirmationText?: string;
  confirmationIcon?: LucideIcon;
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
          <ConfirmationIcon />
          {label}
        </MenuItem>
      </Menu>
    </MenuTrigger>
  );
};
