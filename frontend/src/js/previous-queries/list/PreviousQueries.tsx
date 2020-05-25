import React, { useState } from "react";
import styled from "@emotion/styled";
import ReactList from "react-list";

import PreviousQueryDragContainer from "./PreviousQueryDragContainer";
import { PreviousQueryT } from "./reducer";
import { useDispatch } from "react-redux";
import DeletePreviousQueryModal from "./DeletePreviousQueryModal";
import { deletePreviousQuerySuccess } from "./actions";

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

  const dispatch = useDispatch();
  const closeDeleteModal = () => setPreviousQueryToDelete(null);

  function onDeleteSuccess() {
    if (previousQueryToDelete) {
      dispatch(deletePreviousQuerySuccess(previousQueryToDelete));
    }

    closeDeleteModal();
  }

  function renderQuery(index: number, key: string | number) {
    return (
      <Container key={key}>
        <PreviousQueryDragContainer
          query={queries[index]}
          datasetId={datasetId}
          onIndicateDeletion={() => setPreviousQueryToDelete(queries[index].id)}
        />
      </Container>
    );
  }

  return (
    <Root>
      {!!previousQueryToDelete && (
        <DeletePreviousQueryModal
          previousQueryId={previousQueryToDelete}
          onClose={closeDeleteModal}
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
