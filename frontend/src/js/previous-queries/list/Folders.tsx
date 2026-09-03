import { faPlus, faTimes } from "@fortawesome/free-solid-svg-icons";
import { useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../../app/reducers";
import { DNDType } from "../../common/constants/dndTypes";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";
import type { DragItemFormConfig } from "../../external-forms/types";
import type { DragItemQuery } from "../../standard-query-editor/types";
import { Button } from "../../ui-components/Button";
import Dropzone from "../../ui-components/Dropzone";
import { Icon } from "../../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";
import {
  removeFolderFromFilter,
  setFolderFilter,
  toggleNoFoldersFilter,
} from "../folder-filter/actions";

import AddFolderModal from "./AddFolderModal";
import {
  addFolder,
  removeFolder,
  useUpdateFormConfig,
  useUpdateQuery,
} from "./actions";
import DeleteFolderModal from "./DeleteFolderModal";
import Folder from "./Folder";
import { useFolders } from "./selector";

const DROP_TYPES = [
  DNDType.FORM_CONFIG,
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const root = tv({
  base: ["flex flex-col items-start", "shrink-0", "h-full", "overflow-hidden"],
});

// shown while the surrounding folder dropzone (group/folder) is hovered;
// invisible rather than hidden, so it keeps its layout and its tooltip stays anchored
const deleteButton = tv({
  base: [
    "absolute top-0 right-0",
    "invisible group-hover/folder:visible",
    "bg-bg-50",
    "rounded-none",
  ],
});

const folderDropzone = tv({
  base: [
    "group/folder",
    "relative",
    "justify-start",
    "mb-[2px]",
    "cursor-pointer",
    "hover:bg-gray-50",
  ],
});

const scrollContainer = tv({
  base: [
    "flex flex-col items-start",
    "grow",
    "w-full",
    "overflow-y-auto overflow-x-hidden",
  ],
});

const NARROW_WIDTH = 120;
const useIsParentNarrow = () => {
  // TODO: Once https://caniuse.com/css-container-queries ships, use those instead
  const parentRef = useRef<HTMLDivElement | null>(null);
  const [parentWidth, setParentWidth] = useState<number>(0);
  const isNarrow = parentWidth < NARROW_WIDTH;
  useResizeObserver(
    useCallback((entry: ResizeObserverEntry) => {
      if (entry) {
        setParentWidth(entry.contentRect.width);
      }
    }, []),
    parentRef.current,
  );

  return {
    isNarrow,
    parentRef,
  };
};

const Folders = ({ className }: { className?: string }) => {
  const folders = useFolders();
  const localFolders = useSelector<StateT, string[]>(
    (state) => state.previousQueries.localFolders,
  );
  const folderFilter = useSelector<StateT, string[]>(
    (state) => state.previousQueriesFolderFilter.folders,
  );
  const noFoldersActive = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.noFoldersActive,
  );
  const searchResult = useSelector<StateT, Record<string, number> | null>(
    (state) => state.projectItemsSearch.result,
  );
  const searchResultWords = useSelector<StateT, string[]>(
    (state) => state.projectItemsSearch.words,
  );

  const { t } = useTranslation();
  const dispatch = useDispatch();
  const onToggleNoFoldersActive = () => dispatch(toggleNoFoldersFilter());
  const onResetFolderFilter = useCallback(
    () => dispatch(setFolderFilter([])),
    [dispatch],
  );

  const onClickFolder = (folder: string) => {
    if (!folderFilter.includes(folder)) {
      dispatch(setFolderFilter([folder]));
    } else {
      dispatch(removeFolderFromFilter(folder));
    }
  };

  const { updateQuery } = useUpdateQuery();
  const { updateFormConfig } = useUpdateFormConfig();
  const onDropIntoFolder = async (
    item: DragItemQuery | DragItemFormConfig,
    folder: string,
  ) => {
    if (item.tags.includes(folder)) {
      return;
    }

    if (item.type === DNDType.FORM_CONFIG) {
      await updateFormConfig(
        item.id,
        { tags: [...item.tags, folder] },
        t("formConfig.retagError"),
      );
    } else {
      await updateQuery(
        item.id,
        { tags: [...item.tags, folder] },
        t("previousQuery.retagError"),
      );
    }

    // Delete from the temporary "localFolders", because now it's a "real" folder
    if (localFolders.includes(folder)) {
      dispatch(removeFolder({ folderName: folder }));
    }
  };

  const [folderToDelete, setFolderToDelete] = useState<string | null>(null);
  const [showAddFolderModal, setShowAddFolderModal] = useState<boolean>(false);

  const { isNarrow, parentRef } = useIsParentNarrow();

  useEffect(
    function resetFolderFilterWhenFolderNotVisible() {
      const isSomeActiveFolderInvisible = folderFilter.some(
        (folder) => !folders.includes(folder),
      );

      if (isSomeActiveFolderInvisible) {
        onResetFolderFilter();
      }
    },
    [folders, onResetFolderFilter, folderFilter],
  );

  return (
    <div className={root({ className })}>
      {folderToDelete && (
        <DeleteFolderModal
          folder={folderToDelete}
          onClose={() => setFolderToDelete(null)}
          onDeleteSuccess={() => {
            setFolderToDelete(null);
            dispatch(setFolderFilter([]));
          }}
        />
      )}
      <div
        className="mb-3 flex w-full min-w-[100px] items-start"
        ref={parentRef}
      >
        <Button
          intent="tertiary"
          size="sm"
          onPress={() => setShowAddFolderModal(true)}
          className="text-left"
        >
          <Icon icon={faPlus} />
          {isNarrow ? t("folders.addShort") : t("folders.add")}
        </Button>
      </div>
      {showAddFolderModal && (
        <AddFolderModal
          onClose={() => setShowAddFolderModal(false)}
          isValidName={(v) => v.length > 0 && !folders.includes(v)}
          onSubmit={(v) => {
            if (v.length > 0) {
              setShowAddFolderModal(false);
              dispatch(addFolder({ folderName: v }));
            }
          }}
        />
      )}
      <Folder
        className="mb-[5px]"
        key="all-queries"
        folder={t("folders.allQueries")}
        active={folderFilter.length === 0 && !noFoldersActive}
        onClick={onResetFolderFilter}
        resultCount={searchResult ? searchResult.__all__ : null}
        resultWords={[]}
      />
      <Folder
        className="mb-[5px]"
        key="no-folder"
        special
        folder={t("folders.noFolders")}
        active={noFoldersActive}
        onClick={onToggleNoFoldersActive}
        resultCount={searchResult ? searchResult.__without_folder__ : null}
        resultWords={[]}
      />
      <div className={scrollContainer()}>
        {folders.map((folder, i) => {
          return (
            <Dropzone
              className={folderDropzone()}
              key={`${folder}-${i}`}
              naked
              bare
              onDrop={(item) =>
                onDropIntoFolder(
                  item as DragItemQuery | DragItemFormConfig,
                  folder,
                )
              }
              acceptedDropTypes={DROP_TYPES}
              canDrop={(item) =>
                (item.type === DNDType.FORM_CONFIG ||
                  item.type === DNDType.PREVIOUS_QUERY ||
                  item.type === DNDType.PREVIOUS_SECONDARY_ID_QUERY) &&
                !!(item.own || item.shared)
              }
              onClick={() => onClickFolder(folder)}
            >
              {() => (
                <>
                  <Folder
                    key={folder}
                    folder={folder}
                    active={folderFilter.includes(folder)}
                    onClick={() => onClickFolder(folder)}
                    resultCount={searchResult ? searchResult[folder] : null}
                    resultWords={searchResultWords}
                  />
                  <TooltipTrigger>
                    <Button
                      size="sm"
                      aria-label={t("common.delete")}
                      intent="tertiary"
                      onPress={() => {
                        setFolderToDelete(folder);
                      }}
                      className={deleteButton()}
                    >
                      <Icon icon={faTimes} />
                    </Button>
                    <Tooltip>{t("common.delete")}</Tooltip>
                  </TooltipTrigger>
                </>
              )}
            </Dropzone>
          );
        })}
      </div>
    </div>
  );
};
export default Folders;
