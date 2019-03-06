import React from "react";
import styled from "@emotion/styled";

import { QueryGroupModal } from "../query-group-modal";
import UploadConceptListModal from "../upload-concept-list-modal/UploadConceptListModal";
import UploadFilterListModal from "../upload-filter-list-modal/UploadFilterListModal";
import type { DatasetIdType } from "../dataset/reducer";

import Query from "./Query";
import StandardQueryNodeEditor from "./StandardQueryNodeEditor";

type PropsType = {
  selectedDatasetId: DatasetIdType
};

const Root = styled("div")`
  flex-grow: 1;
  overflow: auto;
  padding: 0 10px 10px 10px;
`;

export const QueryEditor = (props: PropsType) => (
  <Root>
    <Query selectedDatasetId={props.selectedDatasetId} />
    <StandardQueryNodeEditor datasetId={props.selectedDatasetId} />
    <UploadConceptListModal selectedDatasetId={props.selectedDatasetId} />
    <UploadFilterListModal selectedDatasetId={props.selectedDatasetId} />
    <QueryGroupModal />
  </Root>
);
