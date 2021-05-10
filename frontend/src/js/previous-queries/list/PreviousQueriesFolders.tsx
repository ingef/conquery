import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";

import { updatePreviousQueriesSearch } from "../search/actions";

import PreviousQueriesFolder from "./PreviousQueriesFolder";
import type { PreviousQueryT } from "./reducer";

const WIDTH_OPEN = 150;

const Folders = styled("div")<{ isOpen?: boolean }>`
  transition: margin ${({ theme }) => theme.transitionTime},
    width ${({ theme }) => theme.transitionTime};
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

  return (
    <Folders isOpen={isOpen}>
      {folders.map((folder) => (
        <PreviousQueriesFolder
          key={folder}
          folder={folder}
          onClick={() => onClickFolder(folder)}
        />
      ))}
    </Folders>
  );
};
export default PreviousQueriesFolders;
