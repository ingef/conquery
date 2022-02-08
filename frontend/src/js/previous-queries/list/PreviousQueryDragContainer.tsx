import { FC, useRef } from "react";
import { useDrag } from "react-dnd";

import { DatasetIdT } from "../../api/types";
import { getWidthAndHeight } from "../../app/DndProvider";
import { DNDType } from "../../common/constants/dndTypes";
import type { DragItemQuery } from "../../standard-query-editor/types";

import PreviousQuery from "./PreviousQuery";
import { PreviousQueryT } from "./reducer";

interface PropsT {
  query: PreviousQueryT;
  datasetId: DatasetIdT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
  onIndicateEditFolders: () => void;
}

const getDragType = (query: PreviousQueryT) => {
  return query.queryType === "CONCEPT_QUERY"
    ? DNDType.PREVIOUS_QUERY
    : DNDType.PREVIOUS_SECONDARY_ID_QUERY;
};

const PreviousQueryDragContainer: FC<PropsT> = ({ query, ...props }) => {
  const ref = useRef<HTMLDivElement | null>(null);

  const item: DragItemQuery = {
    dragContext: {
      width: 0,
      height: 0,
    },
    type: getDragType(query),
    id: query.id,
    label: query.label,
    canExpand: query.canExpand,
    tags: query.tags,
    own: query.own,
    shared: query.shared,
  };

  const [, drag] = useDrag<DragItemQuery, void, {}>({
    type: item.type,
    item: () => ({
      ...item,
      dragContext: {
        ...item.dragContext,
        ...getWidthAndHeight(ref),
      },
    }),
  });

  return (
    <PreviousQuery
      ref={(instance) => {
        ref.current = instance;
        drag(instance);
      }}
      query={query}
      {...props}
    />
  );
};

export default PreviousQueryDragContainer;
