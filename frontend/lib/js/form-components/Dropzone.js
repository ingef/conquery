import React from "react";
import styled from "@emotion/styled";
import { DropTarget } from "react-dnd";

const Root = styled("div")`
  border: 3px
    ${({ theme, isOver, canDrop }) =>
      isOver && !canDrop
        ? `solid ${theme.col.red}`
        : isOver
        ? `solid ${theme.col.black}`
        : `dashed ${theme.col.gray}`};
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  color: ${({ theme, isOver, canDrop }) =>
    isOver && !canDrop
      ? theme.col.red
      : isOver
      ? theme.col.black
      : theme.col.gray};
`;

type InnerZonePropsType = {
  className?: string,
  children?: string,
  connectDropTarget: Function,
  isOver: boolean,
  dropzoneText: string,
  hasItem: boolean
};

export const InnerZone = ({
  children,
  className,
  onClick,
  isOver,
  canDrop,
  connectDropTarget
}: InnerZonePropsType) => {
  return (
    <Root
      ref={instance => connectDropTarget(instance)}
      isOver={isOver}
      canDrop={canDrop}
      className={className}
      onClick={onClick}
    >
      {children}
    </Root>
  );
};

type PropsType = {
  acceptedDropTypes: string[],
  onDrop: (Object, Object) => void,
  target?: Object
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver(),
  canDrop: monitor.canDrop()
});

const Dropzone = ({
  acceptedDropTypes,
  onDrop,
  target,
  ...restProps
}: PropsType) => {
  const dropzoneTarget = { drop: onDrop, ...target };

  const FinalZone = DropTarget(acceptedDropTypes, dropzoneTarget, collect)(
    InnerZone
  );

  return <FinalZone {...restProps} />;
};

export default Dropzone;
