import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import Dropzone, { DropzoneProps } from "../../form-components/Dropzone";
import type { DragItemQuery } from "../../standard-query-editor/types";
import {
  removeFolderFromFilter,
  setFolderFilter,
  toggleNoFoldersFilter,
} from "../folderFilter/actions";

import PreviousQueriesFolder from "./PreviousQueriesFolder";
import { useRetagPreviousQuery } from "./actions";
import type { PreviousQueryT } from "./reducer";

const WIDTH_OPEN = 150;

const Folders = styled("div")<{ isOpen?: boolean }>`
  flex-shrink: 0;
  height: 100%;
  overflow: hidden;
  padding: 8px 0px;
  width: 0;
  border-right: none;
  display: flex;
  align-items: flex-start;
  flex-direction: column;

  ${({ isOpen }) =>
    isOpen &&
    css`
      width: ${WIDTH_OPEN}px;
      padding: 8px 12px 4px 0;
    `};
`;

const SxDropzone = styled(Dropzone)`
  justify-content: flex-start;
  margin-bottom: 2px;
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
  overflow-x: auto;
  flex-grow: 1;

  display: flex;
  align-items: flex-start;
  flex-direction: column;
`;

interface Props {
  className?: string;
  isOpen?: boolean;
}

const PreviousQueriesFolders: FC<Props> = ({ isOpen, className }) => {
  const queries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const folders = useMemo(
    () => Array.from(new Set(queries.flatMap((query) => query.tags))).sort(),
    [queries],
  );
  const folderFilter = useSelector<StateT, string[]>(
    (state) => state.previousQueriesFolderFilter.folders,
  );
  const noFoldersActive = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.noFoldersActive,
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

  const retagPreviousQuery = useRetagPreviousQuery();
  const onDropIntoFolder = (query: DragItemQuery, folder: string) => {
    if (query.tags.includes(folder)) {
      return;
    }

    retagPreviousQuery(query.id, Array.from(new Set([...query.tags, folder])));
  };

  return (
    <Folders className={className} isOpen={isOpen}>
      <SmallLabel>{t("folders.headline")}</SmallLabel>
      <ScrollContainer>
        <SxPreviousQueriesFolder
          key="all-queries"
          folder={t("folders.allQueries")}
          active={folderFilter.length === 0 && !noFoldersActive}
          onClick={onResetFolderFilter}
        />
        <SxPreviousQueriesFolder
          key="no-folder"
          empty
          folder={t("folders.noFolders")}
          active={noFoldersActive}
          onClick={onToggleNoFoldersActive}
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
          >
            {() => (
              <PreviousQueriesFolder
                key={folder}
                folder={folder}
                active={folderFilter.includes(folder)}
                onClick={() => onClickFolder(folder)}
              />
            )}
          </SxDropzone>
        ))}
      </ScrollContainer>
    </Folders>
  );
};
export default PreviousQueriesFolders;
