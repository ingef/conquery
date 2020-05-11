import React from "react";
import styled from "@emotion/styled";
import { DropTarget } from "react-dnd";

const Root = styled("div")<{
  isOver?: boolean;
  canDrop?: boolean;
}>`
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

export type ChildArgs = {
  isOver: boolean;
  canDrop: boolean;
  itemType: String;
};

type InnerZonePropsType = {
  className?: string;
  children: (args: ChildArgs) => React.ReactNode;
  connectDropTarget: Function;
  isOver: boolean;
  canDrop: boolean;
  itemType: string;
  onClick: () => void;
};

export const InnerZone = ({
  children,
  className,
  onClick,
  isOver,
  canDrop,
  itemType,
  connectDropTarget,
}: InnerZonePropsType) => {
  return (
    <Root
      ref={(instance) => connectDropTarget(instance)}
      isOver={isOver}
      canDrop={canDrop}
      className={className}
      onClick={onClick}
    >
      {children({ isOver, canDrop, itemType })}
    </Root>
  );
};

type PropsType = {
  acceptedDropTypes: string[];
  children?: (args: ChildArgs) => React.ReactNode;
  onDrop: (props: any, monitor: any) => void;
  target?: Object;
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver(),
  canDrop: monitor.canDrop(),
  itemType: monitor.getItemType(),
});

const Dropzone = ({
  acceptedDropTypes,
  onDrop,
  target,
  ...restProps
}: PropsType) => {
  const dropzoneTarget = { drop: onDrop, ...target };

  const FinalZone = DropTarget(
    acceptedDropTypes,
    dropzoneTarget,
    collect
  )(InnerZone);

  return <FinalZone {...restProps} />;
};

export default Dropzone;
