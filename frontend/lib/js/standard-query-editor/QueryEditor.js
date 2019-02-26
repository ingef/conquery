import React from "react";

import { QueryGroupModal } from "../query-group-modal";
import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";
import UploadFilterListModal from "../upload-filter-list-modal/UploadFilterListModal";
import type { DatasetIdType } from "../dataset/reducer";

import Query from "./Query";
import StandardQueryNodeEditor from "./StandardQueryNodeEditor";

type PropsType = {
  selectedDatasetId: DatasetIdType
};

export const QueryEditor = (props: PropsType) => (
  <div className="query-editor">
    <Query selectedDatasetId={props.selectedDatasetId} />
    <StandardQueryNodeEditor datasetId={props.selectedDatasetId} />
    <UploadConceptListModal selectedDatasetId={props.selectedDatasetId} />
    <UploadFilterListModal selectedDatasetId={props.selectedDatasetId} />
    <QueryGroupModal />
  </div>
);
