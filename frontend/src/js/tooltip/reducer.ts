import { ActionType, getType } from "typesafe-actions";

import type { DateRangeT } from "../api/types";
import type { Action } from "../app/actions";

import {
  displayAdditionalInfos,
  toggleAdditionalInfos,
  toggleDisplayTooltip,
} from "./actions";

type InfoType = {
  key: string;
  value: string;
};

export type AdditionalInfosType = {
  label: string | null;
  description?: string;
  isFolder: boolean;
  matchingEntries: number | null;
  matchingEntities: number | null;
  dateRange?: DateRangeT;
  infos?: InfoType[];
  parent?: string | null;
};

export type TooltipStateT = {
  displayTooltip: boolean;
  toggleAdditionalInfos: boolean;
  additionalInfos: AdditionalInfosType;
};

const additionalInfosInitialState: AdditionalInfosType = {
  label: null,
  description: undefined,
  isFolder: false,
  matchingEntries: null,
  matchingEntities: null,
  dateRange: undefined,
  infos: undefined,
};

const initialState: TooltipStateT = {
  displayTooltip: true,
  toggleAdditionalInfos: false,
  additionalInfos: additionalInfosInitialState,
};

const setAdditionalInfos = (
  state: TooltipStateT,
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

const tooltip = (
  state: TooltipStateT = initialState,
  action: Action,
): TooltipStateT => {
  switch (action.type) {
    case getType(displayAdditionalInfos):
      return setAdditionalInfos(state, action.payload);
    case getType(toggleAdditionalInfos):
      return {
        ...state,
        toggleAdditionalInfos: !state.toggleAdditionalInfos,
      };
    case getType(toggleDisplayTooltip):
      return {
        ...state,
        displayTooltip: !state.displayTooltip,
      };
    default:
      return state;
  }
};

export default tooltip;
