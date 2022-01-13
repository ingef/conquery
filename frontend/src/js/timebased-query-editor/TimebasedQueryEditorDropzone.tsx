import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { DNDType } from "../common/constants/dndTypes";
import { exists } from "../common/helpers/exists";
import type { DragItemQuery } from "../standard-query-editor/types";
import Dropzone from "../ui-components/Dropzone";

import { removeTimebasedNode } from "./actions";
import { TimebasedResultType } from "./reducer";

interface PropsType {
  onDropNode: (
    node: TimebasedResultType | DragItemQuery,
    moved: boolean,
  ) => void;
}

const StyledDropzone = styled(Dropzone)`
  width: 150px;
  text-align: center;
`;

const DROP_TYPES = [DNDType.PREVIOUS_QUERY];

const TimebasedQueryEditorDropzone = ({ onDropNode }: PropsType) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const onRemoveTimebasedNode = (
    conditionIdx: number,
    resultIdx: number,
    moved: boolean,
  ) => dispatch(removeTimebasedNode({ conditionIdx, resultIdx, moved }));

  const onDrop = (item: DragItemQuery) => {
    const { movedFromAndIdx, movedFromOrIdx } = item.dragContext;

    if (exists(movedFromAndIdx) && exists(movedFromOrIdx)) {
      onRemoveTimebasedNode(movedFromAndIdx, movedFromOrIdx, true);
      onDropNode(item, true);
    } else {
      onDropNode(item, false);
    }
  };

  return (
    <StyledDropzone /* TODO: ADD GENERIC TYPE <FC<DropzoneProps<DragItemQuery>>> */
      acceptedDropTypes={DROP_TYPES}
      onDrop={(item) => onDrop(item as DragItemQuery)}
    >
      {() => t("dropzone.dragQuery")}
    </StyledDropzone>
  );
};

export default TimebasedQueryEditorDropzone;
