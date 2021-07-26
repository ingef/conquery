import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import IconButton from "../../button/IconButton";
import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import Dropzone, { DropzoneProps } from "../../form-components/Dropzone";
import type { DragItemQuery } from "../../standard-query-editor/types";
import WithTooltip from "../../tooltip/WithTooltip";
import {
  removeFolderFromFilter,
  setFolderFilter,
  toggleNoFoldersFilter,
} from "../folderFilter/actions";

import DeletePreviousQueryFolderModal from "./DeletePreviousQueryFolderModal";
import PreviousQueriesFolder from "./PreviousQueriesFolder";
import { useRetagQuery } from "./actions";
import { usePreviousQueriesTags } from "./selector";

const Folders = styled("div")`
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

const SxWithTooltip = styled(WithTooltip)`
  position: absolute;
  right: 0px;
  top: 0px;
  display: none !important; /* to override display: inline */
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

const SxPreviousQueriesFolder = styled(PreviousQueriesFolder)`
  margin-bottom: 10px;
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

interface Props {
  className?: string;
}

const PreviousQueriesFolders: FC<Props> = ({ className }) => {
  const folders = usePreviousQueriesTags();
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
  const onResetFolderFilter = () => dispatch(setFolderFilter([]));

  const onClickFolder = (folder: string) => {
    if (!folderFilter.includes(folder)) {
      dispatch(setFolderFilter([folder]));
    } else {
      dispatch(removeFolderFromFilter(folder));
    }
  };

  const retagQuery = useRetagQuery();
  const onDropIntoFolder = (query: DragItemQuery, folder: string) => {
    if (query.tags.includes(folder)) {
      return;
    }

    retagQuery(query.id, [...query.tags, folder]);
  };

  const [folderToDelete, setFolderToDelete] = useState<string | null>(null);

  return (
    <Folders className={className}>
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
        {folders.map((folder, i) => (
          <SxDropzone<FC<DropzoneProps<DragItemQuery>>>
            key={`${folder}-${i}`}
            naked
            onDrop={(item) => onDropIntoFolder(item, folder)}
            acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
            canDrop={(item) =>
              (item.type === "PREVIOUS_QUERY" ||
                item.type === "PREVIOUS_SECONDARY_ID_QUERY") &&
              (item.own || item.shared)
            }
            onClick={() => onClickFolder(folder)}
          >
            {() => (
              <>
                <PreviousQueriesFolder
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
        ))}
      </ScrollContainer>
    </Folders>
  );
};
export default PreviousQueriesFolders;
