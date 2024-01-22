import styled from "@emotion/styled";
import { IconProp } from "@fortawesome/fontawesome-svg-core";
import { faCheck } from "@fortawesome/free-solid-svg-icons";
import { ReactElement, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Instance } from "tippy.js";

import IconButton from "../button/IconButton";

import WithTooltip from "./WithTooltip";

const List = styled("div")`
  display: flex;
  flex-direction: column;
`;

const SxIconButton = styled(IconButton)`
  width: 100%;
  padding: 8px 14px;
`;

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
      <List>
        <SxIconButton
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
        >
          {confirmationText || t("common.confirm")}
        </SxIconButton>
      </List>
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
