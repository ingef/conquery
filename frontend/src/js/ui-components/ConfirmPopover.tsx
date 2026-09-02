import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import type { ComponentProps } from "react";
import { DialogTrigger, Pressable } from "react-aria-components";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";

import { Dialog, Popover } from "./Popover";

const confirmButton = tv({ base: ["w-full", "px-[14px] py-2"] });

/**
 * Asks for confirmation in a popover before calling onConfirm.
 * The child is the trigger, a button.
 */
export const ConfirmPopover = ({
  children,
  confirmationIcon,
  confirmationText,
  placement = "top",
  onConfirm,
  red,
}: {
  children: ComponentProps<typeof Pressable>["children"];
  confirmationText?: string;
  confirmationIcon?: IconProp;
  placement?: "top" | "bottom" | "left" | "right";
  onConfirm: () => void;
  red?: boolean;
}) => {
  const { t } = useTranslation();
  const label = confirmationText || t("common.confirm");

  return (
    <DialogTrigger>
      <Pressable>{children}</Pressable>
      <Popover placement={placement} offset={5}>
        <Dialog aria-label={label}>
          {({ close }) => (
            <IconButton
              className={confirmButton()}
              icon={confirmationIcon || faCheck}
              onClick={() => {
                onConfirm();
                close();
              }}
              small
              bgHover
              red={red}
              data-test-id="confirm"
            >
              {label}
            </IconButton>
          )}
        </Dialog>
      </Popover>
    </DialogTrigger>
  );
};
