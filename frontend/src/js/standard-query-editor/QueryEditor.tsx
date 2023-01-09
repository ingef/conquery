import styled from "@emotion/styled";
import { useCallback, useEffect, useState } from "react";

import { useDatasetId } from "../dataset/selectors";

import Query from "./Query";
import StandardQueryNodeEditor from "./StandardQueryNodeEditor";

const Root = styled("div")`
  flex-grow: 1;
  height: 100%;
  padding: 8px 10px 10px 10px;
  overflow: hidden;
`;

export const QueryEditor = () => {
  const [editedNode, setEditedNode] = useState<{
    andIdx: number;
    orIdx: number;
  } | null>(null);

  const datasetId = useDatasetId();
  const onClose = useCallback(() => setEditedNode(null), []);

  useEffect(() => {
    onClose();
  }, [datasetId, onClose]);

  return (
    <Root>
      <Query setEditedNode={setEditedNode} />
      {editedNode && (
        <StandardQueryNodeEditor editedNode={editedNode} onClose={onClose} />
      )}
    </Root>
  );
};
