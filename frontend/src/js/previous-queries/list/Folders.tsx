import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC, useCallback, useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import IconButton from "../../button/IconButton";
import { DNDType } from "../../common/constants/dndTypes";
import { useResizeObserver } from "../../common/helpers/useResizeObserver";
import type { DragItemQuery } from "../../standard-query-editor/types";
import WithTooltip from "../../tooltip/WithTooltip";
import Dropzone from "../../ui-components/Dropzone";
import {
  removeFolderFromFilter,
  setFolderFilter,
  toggleNoFoldersFilter,
} from "../folderFilter/actions";

import AddFolderModal from "./AddFolderModal";
import DeletePreviousQueryFolderModal from "./DeletePreviousQueryFolderModal";
import Folder from "./Folder";
import { addFolder, useUpdateQuery } from "./actions";
import { useFolders } from "./selector";

const DROP_TYPES = [
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
`;

const AddFolderIconButton = styled(IconButton)`
  text-align: left;
  padding: 4px 6px;
`;

const SxWithTooltip = styled(WithTooltip)`
  position: absolute;
  right: 0px;
  top: 0px;
  display: none !important; /* to override display: inline */
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
    ${SxWithTooltip} {
      display: inherit !important; /* to override display: inline */
    }
  }
`;

const SxPreviousQueriesFolder = styled(Folder)`
  margin-bottom: 5px;
`;

const SmallLabel = styled("p")`
  margin: 0;
  padding: 0 5px 10px;
  text-transform: uppercase;
  font-size: ${({ theme }) => theme.font.xs};
  font-weight: 400;
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
  const folderFilter = useSelector<StateT, string[]>(
    (state) => state.previousQueriesFolderFilter.folders,
  );
  const noFoldersActive = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.noFoldersActive,
  );
  const searchResult = useSelector<StateT, Record<string, number> | null>(
    (state) => state.previousQueriesSearch.result,
  );
  const searchResultWords = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch.words,
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
  const onDropIntoFolder = (query: DragItemQuery, folder: string) => {
    if (query.tags.includes(folder)) {
      return;
    }

    updateQuery(
      query.id,
      { tags: [...query.tags, folder] },
      t("previousQueries.retagError"),
    );
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
        <DeletePreviousQueryFolderModal
          folder={folderToDelete}
          onClose={() => setFolderToDelete(null)}
          onDeleteSuccess={() => {
            setFolderToDelete(null);
            dispatch(setFolderFilter([]));
          }}
        />
      )}
      <SmallLabel>{t("folders.headline")}</SmallLabel>
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
      <ScrollContainer>
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
        {folders.map((folder, i) => {
          return (
            <SxDropzone /* TODO: ADD GENERIC TYPE <FC<DropzoneProps<DragItemQuery>>> */
              key={`${folder}-${i}`}
              naked
              onDrop={(item) => onDropIntoFolder(item as DragItemQuery, folder)}
              acceptedDropTypes={DROP_TYPES}
              canDrop={(item) =>
                (item.type === DNDType.PREVIOUS_QUERY ||
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
                  <SxWithTooltip text={t("common.delete")}>
                    <SxIconButton
                      icon="times"
                      onClick={(e) => {
                        setFolderToDelete(folder);
                        e.stopPropagation();
                      }}
                    />
                  </SxWithTooltip>
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
