import React, { FC, useRef } from "react";

import { useDrag } from "react-dnd";

import { PREVIOUS_QUERY } from "../../common/constants/dndTypes";
import { DatasetIdT } from "../../api/types";

import { getWidthAndHeight } from "../../app/DndProvider";
import type { DraggedQueryType } from "../../standard-query-editor/types";
import { PreviousQueryT } from "./reducer";
import PreviousQuery from "./PreviousQuery";

interface PropsT {
  query: PreviousQueryT;
  datasetId: DatasetIdT;
  onIndicateDeletion: () => void;
  onIndicateShare: () => void;
}

const PreviousQueryDragContainer: FC<PropsT> = ({ query, ...props }) => {
  const isNotEditing = !(query.editingLabel || query.editingTags);
  const ref = useRef<HTMLDivElement | null>(null);
  const dragType =
    query.queryType === "CONCEPT_QUERY" ? PREVIOUS_QUERY : "UNDROPPABLE";
  const item = {
    width: 0,
    height: 0,
    type: dragType,
    id: query.id,
    label: query.label,
    isPreviousQuery: true,
  };

  const [, drag] = useDrag({
    item,
    canDrag: dragType !== "UNDROPPABLE",
    begin: (): DraggedQueryType => ({
      ...item,
      ...getWidthAndHeight(ref),
    }),
  });

  return (
    <PreviousQuery
      ref={(instance) => {
        ref.current = instance;
        if (isNotEditing) {
          drag(instance);
        }
      }}
      query={query}
      {...props}
    />
  );
};

export default PreviousQueryDragContainer;
