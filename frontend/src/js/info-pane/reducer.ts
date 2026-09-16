import type { IconDefinition } from "@fortawesome/free-solid-svg-icons";
import { type ActionType, getType } from "typesafe-actions";

import type { DateRangeT } from "../api/types";
import type { Action } from "../app/actions";

import {
  displayAdditionalInfos,
  toggleAdditionalInfos,
  toggleInfoPane,
} from "./actions";

type InfoType = {
  key: string;
  value: string;
};

export type AdditionalInfosType = {
  label?: string;
  description?: string;
  matchingEntries: number | null;
  matchingEntities: number | null;
  dateRange?: DateRangeT;
  infos?: InfoType[];
  isStructNode?: boolean;
  icon?: IconDefinition;
  rootIcon?: IconDefinition;
  rootLabel?: string;
};

export type InfoPaneStateT = {
  isOpen: boolean;
  toggleAdditionalInfos: boolean;
  additionalInfos: AdditionalInfosType;
};

const additionalInfosInitialState: AdditionalInfosType = {
  matchingEntries: null,
  matchingEntities: null,
};

const initialState: InfoPaneStateT = {
  isOpen: true,
  toggleAdditionalInfos: false,
  additionalInfos: additionalInfosInitialState,
};

const setAdditionalInfos = (
  state: InfoPaneStateT,
  { additionalInfos }: ActionType<typeof displayAdditionalInfos>["payload"],
) => {
  if (state.toggleAdditionalInfos)
    return {
      ...state,
    };

  return {
    ...state,
    additionalInfos: additionalInfos || additionalInfosInitialState,
  };
};

const infoPane = (
  state: InfoPaneStateT = initialState,
  action: Action,
): InfoPaneStateT => {
  switch (action.type) {
    case getType(displayAdditionalInfos):
      return setAdditionalInfos(state, action.payload);
    case getType(toggleAdditionalInfos):
      return {
        ...state,
        toggleAdditionalInfos: !state.toggleAdditionalInfos,
      };
    case getType(toggleInfoPane):
      return {
        ...state,
        isOpen: !state.isOpen,
      };
    default:
      return state;
  }
};

export default infoPane;
