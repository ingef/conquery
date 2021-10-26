import styled from "@emotion/styled";
import { useRef, useState, useCallback, useLayoutEffect, FC } from "react";
import { FixedSizeList } from "react-window";

import { DatasetIdT } from "../../api/types";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";

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
const ROOT_PADDING_Y = 4;

const Root = styled("div")`
  flex-grow: 1;
  font-size: ${({ theme }) => theme.font.sm};
  padding: ${ROOT_PADDING_Y}px 0;
`;
const Container = styled("div")``;

const PreviousQueries: FC<PropsT> = ({ datasetId, queries }) => {
  const [previousQueryToDelete, setPreviousQueryToDelete] = useState<
    string | null
  >(null);
  const [previousQueryToShare, setPreviousQueryToShare] = useState<
    string | null
  >(null);
  const [previousQueryToEditFolders, setPreviousQueryToEditFolders] =
    useState<PreviousQueryT | null>(null);

  const onCloseDeleteModal = () => setPreviousQueryToDelete(null);
  const onCloseShareModal = () => setPreviousQueryToShare(null);
  const onCloseEditFoldersModal = () => setPreviousQueryToEditFolders(null);

  const container = useRef<HTMLDivElement | null>(null);
  const [height, setHeight] = useState<number>(0);

  useResizeObserver(
    useCallback((entry: ResizeObserverEntry) => {
      if (entry) {
        setHeight(entry.contentRect.height - ROOT_PADDING_Y * 2);
      }
    }, []),
    container.current,
  );

  useLayoutEffect(() => {
    if (container.current) {
      const rect = container.current.getBoundingClientRect();

      setHeight(rect.height - ROOT_PADDING_Y * 2);
    }
  }, []);

  return (
    <Root
      ref={(instance) => {
        if (!instance) {
          container.current = null;
          return;
        }

        container.current = instance;
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
          height={height}
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
