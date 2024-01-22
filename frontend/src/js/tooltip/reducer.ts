import { IconDefinition } from "@fortawesome/free-solid-svg-icons";
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

export type TooltipStateT = {
  displayTooltip: boolean;
  toggleAdditionalInfos: boolean;
  additionalInfos: AdditionalInfosType;
};

const additionalInfosInitialState: AdditionalInfosType = {
  matchingEntries: null,
  matchingEntities: null,
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
