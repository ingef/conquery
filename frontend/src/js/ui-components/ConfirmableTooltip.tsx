import type { IconProp } from "@fortawesome/fontawesome-svg-core";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import { type ReactElement, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import type { Instance } from "tippy.js";

import IconButton from "../button/IconButton";

import WithTooltip from "./WithTooltip";

const confirmButton = tv({ base: ["w-full", "px-[14px] py-2"] });

const offset = [0, 5] as [number, number];
export const ConfirmableTooltip = ({
  children,
  confirmationIcon,
  confirmationText,
  placement,
  onConfirm,
  red,
}: {
  children: ReactElement;
  confirmationText?: string;
  confirmationIcon?: IconProp;
  placement?: "top" | "bottom" | "left" | "right";
  onConfirm: () => void;
  red?: boolean;
}) => {
  const tippyRef = useRef(null);
  const { t } = useTranslation();
  const dropdown = useMemo(() => {
    return (
      <div className="flex flex-col">
        <IconButton
          className={confirmButton()}
          icon={confirmationIcon || faCheck}
          onClick={() => {
            onConfirm();

            // https://github.com/atomiks/tippyjs-react/issues/324
            // @ts-ignore TODO: Find a better way to get the tippy instance / to hide it
            const tippyInstance = tippyRef.current?._tippy as Instance;
            if (tippyInstance) {
              tippyInstance.hide();
            }
          }}
          small
          bgHover
          red={red}
          data-test-id="confirm"
        >
          {confirmationText || t("common.confirm")}
        </IconButton>
      </div>
    );
  }, [t, confirmationText, confirmationIcon, onConfirm, red]);

  return (
    <WithTooltip
      html={dropdown}
      interactive
      placement={placement}
      arrow={false}
      trigger="click"
      offset={offset}
      hideOnClick
      ref={tippyRef}
    >
      {children}
    </WithTooltip>
  );
};
