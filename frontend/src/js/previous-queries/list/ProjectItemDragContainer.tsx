import { useRef } from "react";
import { useDrag } from "react-dnd";

import { getWidthAndHeight } from "../../app/DndProvider";
import { DNDType } from "../../common/constants/dndTypes";
import type { DragItemFormConfig } from "../../external-forms/types";
import type { DragItemQuery } from "../../standard-query-editor/types";

import ProjectItem, { ProjectItemT } from "./ProjectItem";
import { isFormConfig } from "./helpers";
import { PreviousQueryT } from "./reducer";

const getDragType = (item: PreviousQueryT) => {
  return item.queryType === "CONCEPT_QUERY"
    ? DNDType.PREVIOUS_QUERY
    : DNDType.PREVIOUS_SECONDARY_ID_QUERY;
};

const ProjectItemDragContainer = ({
  item,
  ...props
}: {
  item: ProjectItemT;
  onIndicateShare: () => void;
  onIndicateEditFolders: () => void;
}) => {
  const ref = useRef<HTMLDivElement | null>(null);

  const dragItemBase = {
    dragContext: {
      width: 0,
      height: 0,
    },
    id: item.id,
    label: item.label,
    tags: item.tags,
    own: item.own,
    shared: item.shared,
  };
  const dragItem: DragItemQuery | DragItemFormConfig = isFormConfig(item)
    ? {
        ...dragItemBase,
        type: DNDType.FORM_CONFIG,
      }
    : {
        ...dragItemBase,
        type: getDragType(item),
        canExpand: item.canExpand,
      };

  const [, drag] = useDrag<DragItemQuery | DragItemFormConfig, void, {}>({
    type: dragItem.type,
    item: () => ({
      ...dragItem,
      dragContext: {
        ...dragItem.dragContext,
        ...getWidthAndHeight(ref),
      },
    }),
  });

  return (
    <ProjectItem
      ref={(instance) => {
        ref.current = instance;
        drag(instance);
      }}
      item={item}
      {...props}
    />
  );
};

export default ProjectItemDragContainer;
