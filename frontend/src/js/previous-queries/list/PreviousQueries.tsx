import React, { useState } from "react";
import styled from "@emotion/styled";
import ReactList from "react-list";

import PreviousQueryDragContainer from "./PreviousQueryDragContainer";
import { PreviousQueryT } from "./reducer";
import DeletePreviousQueryModal from "./DeletePreviousQueryModal";
import SharePreviousQueryModal from "./SharePreviousQueryModal";

interface PropsT {
  datasetId: string;
  queries: PreviousQueryT[];
}

const Root = styled("div")`
  flex: 1;
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;
const Container = styled("div")`
  margin: 4px 0;
`;

const PreviousQueries: React.FC<PropsT> = ({ datasetId, queries }) => {
  const [previousQueryToDelete, setPreviousQueryToDelete] = useState<
    string | null
  >(null);
  const [previousQueryToShare, setPreviousQueryToShare] = useState<
    string | null
  >(null);

  const onCloseDeleteModal = () => setPreviousQueryToDelete(null);
  const onCloseShareModal = () => setPreviousQueryToShare(null);

  function onShareSuccess() {
    onCloseShareModal();
  }

  function onDeleteSuccess() {
    onCloseDeleteModal();
  }

  function renderQuery(index: number, key: string | number) {
    return (
      <Container key={key}>
        <PreviousQueryDragContainer
          query={queries[index]}
          datasetId={datasetId}
          onIndicateDeletion={() => setPreviousQueryToDelete(queries[index].id)}
          onIndicateShare={() => setPreviousQueryToShare(queries[index].id)}
        />
      </Container>
    );
  }

  return (
    <Root>
      {!!previousQueryToShare && (
        <SharePreviousQueryModal
          previousQueryId={previousQueryToShare}
          onClose={onCloseShareModal}
          onShareSuccess={onShareSuccess}
        />
      )}
      {!!previousQueryToDelete && (
        <DeletePreviousQueryModal
          previousQueryId={previousQueryToDelete}
          onClose={onCloseDeleteModal}
          onDeleteSuccess={onDeleteSuccess}
        />
      )}
      <ReactList
        itemRenderer={renderQuery}
        length={queries.length}
        type="variable"
      />
    </Root>
  );
};

export default PreviousQueries;
