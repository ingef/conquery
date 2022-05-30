import styled from "@emotion/styled";
import {
  useRef,
  useState,
  useCallback,
  useLayoutEffect,
  FC,
  useEffect,
} from "react";
import { FixedSizeList } from "react-window";

import type { DatasetT } from "../../api/types";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";

import DeleteProjectItemModal from "./DeleteProjectItemModal";
import EditProjectItemFoldersModal from "./EditProjectItemFoldersModal";
import type { ProjectItemT } from "./ProjectItem";
import ProjectItemDragContainer from "./ProjectItemDragContainer";
import ShareProjectItemModal from "./ShareProjectItemModal";

interface PropsT {
  datasetId: DatasetT["id"] | null;
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
  const [itemToDelete, setItemToDelete] = useState<ProjectItemT | null>(null);
  const [itemToShare, setItemToShare] = useState<ProjectItemT | null>(null);
  const [itemToEditFolders, setItemToEditFolders] =
    useState<ProjectItemT | null>(null);

  const onCloseDeleteModal = () => setItemToDelete(null);
  const onCloseShareModal = () => setItemToShare(null);
  const onCloseEditFoldersModal = () => setItemToEditFolders(null);

  const container = useRef<HTMLDivElement | null>(null);
  const [height, setHeight] = useState<number>(0);

  useEffect(
    function updateSelectedItemsOnListUpdate() {
      if (itemToDelete) {
        const updatedItem = items.find((i) => i.id === itemToDelete.id);

        if (updatedItem) setItemToDelete((item) => (item ? updatedItem : null));
      }

      if (itemToEditFolders) {
        const updatedItem = items.find((i) => i.id === itemToEditFolders.id);

        if (updatedItem)
          setItemToEditFolders((item) => (item ? updatedItem : null));
      }

      if (itemToShare) {
        const updatedItem = items.find((i) => i.id === itemToShare.id);

        if (updatedItem) setItemToShare((item) => (item ? updatedItem : null));
      }
    },
    [items, itemToDelete, itemToEditFolders, itemToShare],
  );

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
      data-test-id="project-items-list"
      ref={(instance) => {
        if (!instance) {
          container.current = null;
          return;
        }

        container.current = instance;
      }}
    >
      {!!itemToShare && (
        <ShareProjectItemModal item={itemToShare} onClose={onCloseShareModal} />
      )}
      {!!itemToDelete && (
        <DeleteProjectItemModal
          item={itemToDelete}
          onClose={onCloseDeleteModal}
        />
      )}
      {!!itemToEditFolders && (
        <EditProjectItemFoldersModal
          item={itemToEditFolders}
          onClose={onCloseEditFoldersModal}
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
                  onIndicateDeletion={() => setItemToDelete(items[index])}
                  onIndicateShare={() => setItemToShare(items[index])}
                  onIndicateEditFolders={() =>
                    setItemToEditFolders(items[index])
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
