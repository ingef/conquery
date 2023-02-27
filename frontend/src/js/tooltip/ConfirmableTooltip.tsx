import styled from "@emotion/styled";
import { IconName } from "@fortawesome/fontawesome-svg-core";
import { ReactElement, useMemo } from "react";
import { useTranslation } from "react-i18next";

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
  confirmationIcon?: IconName;
  placement?: "top" | "bottom" | "left" | "right";
  onConfirm: () => void;
  red?: boolean;
}) => {
  const { t } = useTranslation();
  const dropdown = useMemo(() => {
    return (
      <List>
        <SxIconButton
          icon={confirmationIcon || "check"}
          onClick={onConfirm}
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
    >
      {children}
    </WithTooltip>
  );
};
