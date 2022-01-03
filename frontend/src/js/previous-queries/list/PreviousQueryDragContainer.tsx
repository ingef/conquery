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

const PreviousQueryDragContainer: FC<PropsT> = ({ query, ...props }) => {
  const ref = useRef<HTMLDivElement | null>(null);
  const dragType =
    query.queryType === "CONCEPT_QUERY"
      ? DNDType.PREVIOUS_QUERY
      : DNDType.PREVIOUS_SECONDARY_ID_QUERY;

  const item: DragItemQuery = {
    dragContext: {
      width: 0,
      height: 0,
    },
    type: dragType,
    id: query.id,
    label: query.label,
    canExpand: query.canExpand,
    tags: query.tags,
    own: query.own,
    shared: query.shared,
  };

  const [, drag] = useDrag<DragItemQuery, void, {}>({
    item,
    begin: () => ({
      ...item,
      ...getWidthAndHeight(ref),
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
