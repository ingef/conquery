import styled from "@emotion/styled";
import React, { useRef, useState } from "react";
import { FixedSizeList } from "react-window";

import { DatasetIdT } from "../../api/types";

import DeletePreviousQueryModal from "./DeletePreviousQueryModal";
import EditPreviousQueryFoldersModal from "./EditPreviousQueryFoldersModal";
import PreviousQueryDragContainer from "./PreviousQueryDragContainer";
import SharePreviousQueryModal from "./SharePreviousQueryModal";
import { PreviousQueryT } from "./reducer";

interface PropsT {
  datasetId: DatasetIdT | null;
  queries: PreviousQueryT[];
}

const ROW_SIZE = 62;

const Root = styled("div")`
  flex-grow: 1;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 4px 0;
`;
const Container = styled("div")``;

const PreviousQueries: React.FC<PropsT> = ({ datasetId, queries }) => {
  const [previousQueryToDelete, setPreviousQueryToDelete] = useState<
    string | null
  >(null);
  const [previousQueryToShare, setPreviousQueryToShare] = useState<
    string | null
  >(null);
  const [
    previousQueryToEditFolders,
    setPreviousQueryToEditFolders,
  ] = useState<PreviousQueryT | null>(null);

  const onCloseDeleteModal = () => setPreviousQueryToDelete(null);
  const onCloseShareModal = () => setPreviousQueryToShare(null);
  const onCloseEditFoldersModal = () => setPreviousQueryToEditFolders(null);

  const container = useRef<HTMLDivElement | null>(null);
  const height = useRef<number>(0);

  return (
    <Root
      ref={(instance) => {
        if (!instance) {
          container.current = null;
          return;
        }

        container.current = instance;

        // TODO: Detect resize and re-measure
        const rect = instance.getBoundingClientRect();

        height.current = rect.height;
      }}
    >
      {!!previousQueryToShare && (
        <SharePreviousQueryModal
          previousQueryId={previousQueryToShare}
          onClose={onCloseShareModal}
          onShareSuccess={onCloseShareModal}
        />
      )}
      {!!previousQueryToDelete && (
        <DeletePreviousQueryModal
          previousQueryId={previousQueryToDelete}
          onClose={onCloseDeleteModal}
          onDeleteSuccess={onCloseDeleteModal}
        />
      )}
      {!!previousQueryToEditFolders && (
        <EditPreviousQueryFoldersModal
          previousQuery={previousQueryToEditFolders}
          onClose={onCloseEditFoldersModal}
          onEditSuccess={onCloseEditFoldersModal}
        />
      )}
      {datasetId && (
        <FixedSizeList
          key={queries.length}
          itemSize={ROW_SIZE}
          itemCount={queries.length}
          height={height.current}
          width="100%"
        >
          {({ index, style }) => {
            return (
              <Container style={style}>
                <PreviousQueryDragContainer
                  query={queries[index]}
                  datasetId={datasetId}
                  onIndicateDeletion={() =>
                    setPreviousQueryToDelete(queries[index].id)
                  }
                  onIndicateShare={() =>
                    setPreviousQueryToShare(queries[index].id)
                  }
                  onIndicateEditFolders={() =>
                    setPreviousQueryToEditFolders(queries[index])
                  }
                />
              </Container>
            );
          }}
        </FixedSizeList>
      )}
    </Root>
  );
};

export default PreviousQueries;
