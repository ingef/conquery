import styled from "@emotion/styled";
import React from "react";

import QueryUploadConceptListModal from "../query-upload-concept-list-modal/QueryUploadConceptListModal";

import Query from "./Query";
import StandardQueryNodeEditor from "./StandardQueryNodeEditor";

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  padding: 0 10px 10px 10px;
  overflow: hidden;
`;

export const QueryEditor = () => {
  return (
    <Root>
      <Query />
      <StandardQueryNodeEditor />
      <QueryUploadConceptListModal />
    </Root>
  );
};
