import styled from "@emotion/styled";
import type { CSSProperties } from "react";
import { Separator } from "react-resizable-panels";

const SxSeparator = styled(Separator)`
  position: relative;
  background-color: ${({ theme }) => theme.col.grayMediumLight};
  width: 1px;
`;

const Handle = styled("div")`
  position: absolute;
  z-index: 2;
  left: -4px;
  width: 9px;
  top: 0;
  height: 100%;
  padding-left: 4px;

  cursor: col-resize;

  background-color: ${({ theme }) => theme.col.grayVeryLight};
  transition: opacity 0.2s ease-in-out;
  opacity: 0;
  &:hover {
    opacity: 1;
  }
`;

const Line = styled("div")`
  width: 1px;
  height: 100%;

  background-color: ${({ theme }) => theme.col.grayMediumLight};
`;

export const ResizeHandle = ({
  style,
  disabled,
}: {
  style?: CSSProperties;
  disabled?: boolean;
}) => {
  return (
    <SxSeparator style={style} disabled={disabled}>
      <Handle>
        <Line />
      </Handle>
    </SxSeparator>
  );
};
