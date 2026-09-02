import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
} from "react";
import { List, type RowComponentProps } from "react-window";
import { tv } from "tailwind-variants";

import type { DatasetT } from "../../api/types";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";

import EditProjectItemFoldersModal from "./EditProjectItemFoldersModal";
import type { ProjectItemT } from "./ProjectItem";
import ProjectItemDragContainer from "./ProjectItemDragContainer";
import ShareProjectItemModal from "./ShareProjectItemModal";

const ROW_SIZE = 62;

type ProjectItemRowProps = {
  items: ProjectItemT[];
  setItemToShare: (item: ProjectItemT) => void;
  setItemToEditFolders: (item: ProjectItemT) => void;
};

const ProjectItemRow = ({
  index,
  style,
  items,
  setItemToShare,
  setItemToEditFolders,
}: RowComponentProps<ProjectItemRowProps>) => (
  <div style={style}>
    <ProjectItemDragContainer
      item={items[index]}
      onIndicateShare={() => setItemToShare(items[index])}
      onIndicateEditFolders={() => setItemToEditFolders(items[index])}
    />
  </div>
);

// must match root's py-1
const ROOT_PADDING_Y = 4;

const root = tv({
  base: ["grow", "py-1", "text-sm"],
});

export const ProjectItems = ({
  datasetId,
  items,
}: {
  items: ProjectItemT[];
  datasetId: DatasetT["id"] | null;
}) => {
  const [itemToShare, setItemToShare] = useState<ProjectItemT | null>(null);
  const [itemToEditFolders, setItemToEditFolders] =
    useState<ProjectItemT | null>(null);

  const onCloseShareModal = () => setItemToShare(null);
  const onCloseEditFoldersModal = () => setItemToEditFolders(null);

  const container = useRef<HTMLDivElement | null>(null);
  const [height, setHeight] = useState<number>(0);

  useEffect(
    function updateSelectedItemsOnListUpdate() {
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
    [items, itemToEditFolders, itemToShare],
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
    <div
      className={root()}
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
      {!!itemToEditFolders && (
        <EditProjectItemFoldersModal
          item={itemToEditFolders}
          onClose={onCloseEditFoldersModal}
        />
      )}
      {datasetId && (
        <List
          key={items.length}
          rowCount={items.length}
          rowHeight={ROW_SIZE}
          style={{ height, width: "100%" }}
          rowComponent={ProjectItemRow}
          rowProps={{ items, setItemToShare, setItemToEditFolders }}
        />
      )}
    </div>
  );
};
