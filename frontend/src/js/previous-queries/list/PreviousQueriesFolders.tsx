import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";

import {
  PREVIOUS_QUERY,
  PREVIOUS_SECONDARY_ID_QUERY,
} from "../../common/constants/dndTypes";
import Dropzone, { DropzoneProps } from "../../form-components/Dropzone";
import type { DragItemQuery } from "../../standard-query-editor/types";
import { updatePreviousQueriesSearch } from "../search/actions";

import PreviousQueriesFolder from "./PreviousQueriesFolder";
import { useRetagPreviousQuery } from "./actions";
import type { PreviousQueryT } from "./reducer";

const WIDTH_OPEN = 150;

const Folders = styled("div")<{ isOpen?: boolean }>`
  overflow-x: auto;
  flex-shrink: 0;
  height: 100%;
  padding: 4px 0px;
  width: 0;
  border-right: none;
  margin: 4px 0 0;
  display: flex;
  align-items: flex-start;
  flex-direction: column;

  ${({ isOpen }) =>
    isOpen &&
    css`
      width: ${WIDTH_OPEN}px;
      margin: 4px 7px 0 0;
    `};
`;

const SxDropzone = styled(Dropzone)`
  justify-content: flex-start;
`;

interface Props {
  isOpen?: boolean;
}

const PreviousQueriesFolders: FC<Props> = ({ isOpen }) => {
  const queries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const folders = useMemo(
    () => Array.from(new Set(queries.flatMap((query) => query.tags))).sort(),
    [queries],
  );
  const dispatch = useDispatch();
  const onClickFolder = (folder: string) =>
    dispatch(updatePreviousQueriesSearch([folder]));

  const retagPreviousQuery = useRetagPreviousQuery();
  const onDropIntoFolder = (query: DragItemQuery, folder: string) =>
    retagPreviousQuery(query.id, Array.from(new Set([...query.tags, folder])));

  return (
    <Folders isOpen={isOpen}>
      {folders.map((folder, i) => (
        <SxDropzone<FC<DropzoneProps<DragItemQuery>>>
          key={`${folder}-${i}`}
          naked
          onDrop={(item) => onDropIntoFolder(item, folder)}
          acceptedDropTypes={[PREVIOUS_QUERY, PREVIOUS_SECONDARY_ID_QUERY]}
        >
          {() => (
            <PreviousQueriesFolder
              key={folder}
              folder={folder}
              onClick={() => onClickFolder(folder)}
            />
          )}
        </SxDropzone>
      ))}
    </Folders>
  );
};
export default PreviousQueriesFolders;
