import styled from "@emotion/styled";
import { useRef, useState, useCallback, useLayoutEffect, FC } from "react";
import { FixedSizeList } from "react-window";

import { DatasetIdT } from "../../api/types";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";

import DeletePreviousQueryModal from "./DeletePreviousQueryModal";
import EditPreviousQueryFoldersModal from "./EditPreviousQueryFoldersModal";
import type { ProjectItemT } from "./ProjectItem";
import ProjectItemDragContainer from "./ProjectItemDragContainer";
import SharePreviousQueryModal from "./SharePreviousQueryModal";

interface PropsT {
  datasetId: DatasetIdT | null;
  items: ProjectItemT[];
}

const ROW_SIZE = 62;
const ROOT_PADDING_Y = 4;

const Root = styled("div")`
  flex-grow: 1;
  font-size: ${({ theme }) => theme.font.sm};
  padding: ${ROOT_PADDING_Y}px 0;
`;

const ProjectItems: FC<PropsT> = ({ datasetId, items }) => {
  const [previousQueryToDelete, setPreviousQueryToDelete] = useState<
    string | null
  >(null);
  const [previousQueryToShare, setPreviousQueryToShare] = useState<
    string | null
  >(null);
  const [previousQueryToEditFolders, setPreviousQueryToEditFolders] =
    useState<ProjectItemT | null>(null);

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
          key={items.length}
          itemSize={ROW_SIZE}
          itemCount={items.length}
          height={height}
          width="100%"
        >
          {({ index, style }) => {
            return (
              <div style={style}>
                <ProjectItemDragContainer
                  item={items[index]}
                  datasetId={datasetId}
                  onIndicateDeletion={() =>
                    setPreviousQueryToDelete(items[index].id)
                  }
                  onIndicateShare={() =>
                    setPreviousQueryToShare(items[index].id)
                  }
                  onIndicateEditFolders={() =>
                    setPreviousQueryToEditFolders(items[index])
                  }
                />
              </div>
            );
          }}
        </FixedSizeList>
      )}
    </Root>
  );
};

export default ProjectItems;
