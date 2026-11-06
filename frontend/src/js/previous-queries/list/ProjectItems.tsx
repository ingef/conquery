import { useEffect, useState } from "react";
import { List, type RowComponentProps } from "react-window";
import { tv } from "tailwind-variants";

import type { DatasetT } from "../../api/types";

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

const root = tv({
  base: ["grow", "min-h-0", "py-1"],
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

  return (
    <div className={root()} data-test-id="project-items-list">
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
          style={{ height: "100%", width: "100%" }}
          rowComponent={ProjectItemRow}
          rowProps={{ items, setItemToShare, setItemToEditFolders }}
        />
      )}
    </div>
  );
};
