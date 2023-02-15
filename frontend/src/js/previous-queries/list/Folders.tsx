import styled from "@emotion/styled";
import { FC, useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import IconButton from "../../button/IconButton";
import { DNDType } from "../../common/constants/dndTypes";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";
import { DragItemFormConfig } from "../../external-forms/types";
import type { DragItemQuery } from "../../standard-query-editor/types";
import WithTooltip from "../../tooltip/WithTooltip";
import Dropzone from "../../ui-components/Dropzone";
import {
  removeFolderFromFilter,
  setFolderFilter,
  toggleNoFoldersFilter,
} from "../folder-filter/actions";

import AddFolderModal from "./AddFolderModal";
import DeleteFolderModal from "./DeleteFolderModal";
import Folder from "./Folder";
import {
  addFolder,
  removeFolder,
  useUpdateFormConfig,
  useUpdateQuery,
} from "./actions";
import { useFolders } from "./selector";

const DROP_TYPES = [
  DNDType.FORM_CONFIG,
  DNDType.PREVIOUS_QUERY,
  DNDType.PREVIOUS_SECONDARY_ID_QUERY,
];

const Root = styled("div")`
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  border-right: none;
  display: flex;
  align-items: flex-start;
  flex-direction: column;
`;

const SxIconButton = styled(IconButton)`
  background-color: ${({ theme }) => theme.col.bg};
  padding: 2px 8px;
  opacity: 1;
  border-radius: 0;

  position: absolute;
  right: 0px;
  top: 0px;
  display: none;
`;

const AddFolderIconButton = styled(IconButton)`
  text-align: left;
  padding: 4px 6px;
`;

const Row = styled("div")`
  display: flex;
  align-items: flex-start;
  margin-bottom: 12px;
  min-width: 100px;
  width: 100%;
`;

const SxDropzone = styled(Dropzone)`
  justify-content: flex-start;
  margin-bottom: 2px;
  position: relative;
  cursor: pointer;

  &:hover {
    background-color: ${({ theme }) => theme.col.grayVeryLight};
    ${SxIconButton} {
      display: block;
    }
  }
`;

const SxPreviousQueriesFolder = styled(Folder)`
  margin-bottom: 5px;
`;

const ScrollContainer = styled("div")`
  overflow-y: auto;
  overflow-x: hidden;
  flex-grow: 1;
  width: 100%;

  display: flex;
  align-items: flex-start;
  flex-direction: column;
`;

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

interface Props {
  className?: string;
}

const Folders: FC<Props> = ({ className }) => {
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
    <Root className={className}>
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
      <Row ref={parentRef}>
        <AddFolderIconButton
          icon="plus"
          frame
          tight
          onClick={() => setShowAddFolderModal(true)}
        >
          {isNarrow ? t("folders.addShort") : t("folders.add")}
        </AddFolderIconButton>
      </Row>
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
      <SxPreviousQueriesFolder
        key="all-queries"
        folder={t("folders.allQueries")}
        active={folderFilter.length === 0 && !noFoldersActive}
        onClick={onResetFolderFilter}
        resultCount={searchResult ? searchResult["__all__"] : null}
        resultWords={[]}
      />
      <SxPreviousQueriesFolder
        key="no-folder"
        special
        folder={t("folders.noFolders")}
        active={noFoldersActive}
        onClick={onToggleNoFoldersActive}
        resultCount={searchResult ? searchResult["__without_folder__"] : null}
        resultWords={[]}
      />
      <ScrollContainer>
        {folders.map((folder, i) => {
          return (
            <SxDropzone /* TODO: ADD GENERIC TYPE <FC<DropzoneProps<DragItemQuery>>> */
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
                  <WithTooltip text={t("common.delete")}>
                    <SxIconButton
                      icon="times"
                      onClick={(e) => {
                        setFolderToDelete(folder);
                        e.stopPropagation();
                      }}
                    />
                  </WithTooltip>
                </>
              )}
            </SxDropzone>
          );
        })}
      </ScrollContainer>
    </Root>
  );
};
export default Folders;
