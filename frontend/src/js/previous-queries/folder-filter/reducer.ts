import { getType } from "typesafe-actions";

import type { Action } from "../../app/actions";
import { getUserSettings, storeUserSettings } from "../../user/userSettings";

import {
  addFolderToFilter,
  removeFolderFromFilter,
  setFolderFilter,
  toggleFoldersOpen,
  toggleNoFoldersFilter,
} from "./actions";

export type PreviousQueriesFolderFilterStateT = {
  folders: string[];
  noFoldersActive: boolean;
  areFoldersOpen: boolean;
};

const initialState: PreviousQueriesFolderFilterStateT = {
  folders: [],
  noFoldersActive: false,
  areFoldersOpen: getUserSettings().arePreviousQueriesFoldersOpen,
};

const reducer = (
  state: PreviousQueriesFolderFilterStateT = initialState,
  action: Action,
): PreviousQueriesFolderFilterStateT => {
  switch (action.type) {
    case getType(setFolderFilter):
      return { ...state, noFoldersActive: false, folders: action.payload };
    case getType(addFolderToFilter):
      return {
        ...state,
        noFoldersActive: false,
        folders: [...state.folders, action.payload],
      };
    case getType(removeFolderFromFilter):
      const { folders } = state;
      const idx = folders.indexOf(action.payload);

      if (idx === -1) return state;

      return {
        ...state,
        noFoldersActive: false,
        folders: [...folders.slice(0, idx), ...folders.slice(idx + 1)],
      };
    case getType(toggleNoFoldersFilter):
      return { ...state, noFoldersActive: !state.noFoldersActive, folders: [] };
    case getType(toggleFoldersOpen):
      storeUserSettings({
        arePreviousQueriesFoldersOpen: !state.areFoldersOpen,
      });

      return { ...state, areFoldersOpen: !state.areFoldersOpen };
    default:
      return state;
  }
};

export default reducer;
