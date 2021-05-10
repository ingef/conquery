import styled from "@emotion/styled";
import React, { useRef, useState } from "react";
import { FixedSizeList } from "react-window";

import { DatasetIdT } from "../../api/types";

import DeletePreviousQueryModal from "./DeletePreviousQueryModal";
import PreviousQueryDragContainer from "./PreviousQueryDragContainer";
import SharePreviousQueryModal from "./SharePreviousQueryModal";
import { PreviousQueryT } from "./reducer";

interface PropsT {
  datasetId: DatasetIdT | null;
  queries: PreviousQueryT[];
}

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

  const onCloseDeleteModal = () => {
    setPreviousQueryToDelete(null);
  };
  const onCloseShareModal = () => {
    setPreviousQueryToShare(null);
  };

  function onShareSuccess() {
    onCloseShareModal();
  }

  function onDeleteSuccess() {
    onCloseDeleteModal();
  }

  const width = useRef<number>(0);
  const height = useRef<number>(0);

  return (
    <Root
      ref={(instance) => {
        if (!instance) {
          return;
        }

        const rect = instance.getBoundingClientRect();

        width.current = rect.width;
        height.current = rect.height;
      }}
    >
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
      {datasetId && (
        <FixedSizeList
          itemSize={70}
          itemCount={queries.length}
          height={height.current}
          width={width.current}
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
