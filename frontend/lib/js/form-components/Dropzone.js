import React from "react";
import styled from "@emotion/styled";
import { DropTarget } from "react-dnd";

const Root = styled("div")`
  border: 3px
    ${({ theme, over }) =>
      over ? `solid ${theme.col.black}` : `dashed ${theme.col.gray}`};
  border-radius: ${({ theme }) => theme.borderRadius};
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  color: ${({ theme, over }) => (over ? theme.col.black : theme.col.gray)};
`;

type PropsType = {
  className?: string,
  children?: string,
  connectDropTarget: Function,
  isOver: boolean,
  dropzoneText: string,
  hasItem: boolean
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver()
});

class Zone extends React.Component {
  props: PropsType;

  render() {
    const {
      children,
      className,
      onClick,
      isOver,
      connectDropTarget
    } = this.props;

    return (
      <Root
        ref={instance => connectDropTarget(instance)}
        over={isOver}
        className={className}
        onClick={onClick}
      >
        {children}
      </Root>
    );
  }
}

const Dropzone = ({ acceptedDropTypes, onDrop, target, ...restProps }) => {
  const dropzoneTarget = { drop: onDrop, ...target };

  const FinalZone = DropTarget(acceptedDropTypes, dropzoneTarget, collect)(
    Zone
  );

  return <FinalZone {...restProps} />;
};

export default Dropzone;
