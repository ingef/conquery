import React, { FC } from "react";
import styled from "@emotion/styled";

import QueryGroupModal from "../query-group-modal/QueryGroupModal";
import QueryUploadConceptListModal from "../query-upload-concept-list-modal/QueryUploadConceptListModal";
import type { DatasetIdT } from "../api/types";

import Query from "./Query";
import StandardQueryNodeEditor from "./StandardQueryNodeEditor";

interface PropsT {
  selectedDatasetId: DatasetIdT;
}

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  overflow: auto;
  padding: 0 10px 10px 10px;
`;

export const QueryEditor: FC<PropsT> = ({ selectedDatasetId }) => (
  <Root>
    <Query selectedDatasetId={selectedDatasetId} />
    <StandardQueryNodeEditor datasetId={selectedDatasetId} />
    <QueryUploadConceptListModal selectedDatasetId={selectedDatasetId} />
    <QueryGroupModal />
  </Root>
);
