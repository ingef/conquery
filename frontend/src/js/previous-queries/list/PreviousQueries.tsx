import styled from "@emotion/styled";
import React, { useRef, useState } from "react";
import { FixedSizeList, VariableSizeList } from "react-window";

import { DatasetIdT } from "../../api/types";

import DeletePreviousQueryModal from "./DeletePreviousQueryModal";
import PreviousQueryDragContainer from "./PreviousQueryDragContainer";
import SharePreviousQueryModal from "./SharePreviousQueryModal";
import { PreviousQueryT } from "./reducer";

interface PropsT {
  datasetId: DatasetIdT | null;
  queries: PreviousQueryT[];
}

const ROW_SIZE_WITH_TAGS = 87;
const ROW_SIZE_WITHOUT_TAGS = 62;
const AVG_ROW_SIZE = (ROW_SIZE_WITHOUT_TAGS + ROW_SIZE_WITH_TAGS) / 2;

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
      {datasetId && (
        <VariableSizeList
          estimatedItemSize={AVG_ROW_SIZE}
          key={queries.length}
          itemSize={(index) =>
            queries[index].tags.length === 0
              ? ROW_SIZE_WITHOUT_TAGS
              : ROW_SIZE_WITH_TAGS
          }
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
                />
              </Container>
            );
          }}
        </VariableSizeList>
      )}
    </Root>
  );
};

export default PreviousQueries;
