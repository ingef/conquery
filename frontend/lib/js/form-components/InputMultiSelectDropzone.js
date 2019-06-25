// @flow

import { DropTarget } from "react-dnd";
import { NativeTypes } from "react-dnd-html5-backend";

import { InnerZone } from "./Dropzone";

/*
  Can't use the dynamic <Dropzone> from './Dropzone' (the default export),
  because the dynamic generation of DropTargets will lead the nested InputMultiSelect
  to lose focus when re-rendering.

  And we're rerendering an InputMultiSelect potentially quite often.
*/
const target = {
  drop: (props, monitor) => {
    const item = monitor.getItem();

    if (item && props.onDropFile) {
      props.onDropFile(item.files[0]);
    }
  }
};

const collect = (connect, monitor) => ({
  connectDropTarget: connect.dropTarget(),
  isOver: monitor.isOver(),
  canDrop: monitor.canDrop()
});

export default DropTarget([NativeTypes.FILE], target, collect)(InnerZone);
