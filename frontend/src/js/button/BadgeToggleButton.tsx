import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { ReactNode } from "react";
import { useHotkeys } from "react-hotkeys-hook";

import BasicButton from "./BasicButton";

const common = css`
  font-weight: 700;
  padding: 1px 4px;
  white-space: nowrap;
`;

const ActiveButton = styled(BasicButton)`
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 2px solid ${({ theme }) => theme.col.blueGrayDark};
  background-color: white;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.blueGrayDark};
  ${common};

  &:hover {
    background-color: ${({ theme }) => theme.col.grayVeryLight};
  }
`;

const InactiveButton = styled(BasicButton)`
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 2px dotted ${({ theme }) => theme.col.grayLight};
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme }) => theme.col.gray};
  ${common};
  &:hover {
    background-color: ${({ theme }) => theme.col.bg};
  }
`;

const SuperScript = styled("span")`
  padding-left: 3px;
  font-size: ${({ theme }) => theme.font.tiny};
  transform: translate(1px, -2px);
  display: inline-block;
  color: ${({ theme }) => theme.col.gray};
`;

interface Props {
  className?: string;
  active?: boolean;
  onClick: () => void;
  children: ReactNode;
  hotkey?: string;
}

export const BadgeToggleButton = ({
  className,
  active,
  onClick,
  children,
  hotkey,
}: Props) => {
  const Component = active ? ActiveButton : InactiveButton;

  useHotkeys(hotkey || "", onClick, { enabled: !!hotkey }, [hotkey, onClick]);

  return (
    <Component className={className} onClick={onClick}>
      {!active && "+ "}
      {children}
      {hotkey && <SuperScript>{hotkey}</SuperScript>}
    </Component>
  );
};
