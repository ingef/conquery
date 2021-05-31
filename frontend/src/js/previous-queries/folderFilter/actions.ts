import { ActionType, createAction } from "typesafe-actions";

export type FolderFilterActions = ActionType<
  | typeof addFolderToFilter
  | typeof removeFolderFromFilter
  | typeof setFolderFilter
  | typeof toggleNoFoldersFilter
  | typeof toggleFoldersOpen
>;

export const addFolderToFilter = createAction(
  "previous-queries/ADD_FOLDER_FILTER",
)<string>();

export const removeFolderFromFilter = createAction(
  "previous-queries/REMOVE_FOLDER_FILTER",
)<string>();

export const setFolderFilter = createAction(
  "previous-queries/SET_FOLDER_FILTER",
)<string[]>();

export const toggleNoFoldersFilter = createAction(
  "previous-queries/TOGGLE_NO_FOLDERS_FILTER",
)();

export const toggleFoldersOpen = createAction(
  "previous-queries/TOGGLE_FOLDERS_OPEN",
)();
