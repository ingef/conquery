import { DateRangeT } from "../api/types";

import {
  TOGGLE_DISPLAY_TOOLTIP,
  DISPLAY_ADDITIONAL_INFOS,
  TOGGLE_ADDITIONAL_INFOS,
} from "./actionTypes";

type InfoType = {
  key: string;
  value: string;
};

export type AdditionalInfosType = {
  label: string | null;
  description: string | null;
  isFolder: boolean;
  matchingEntries: number | null;
  dateRange: DateRangeT | null;
  infos: InfoType[] | null;
};

export type TooltipStateT = {
  displayTooltip: boolean;
  toggleAdditionalInfos: boolean;
  additionalInfos: AdditionalInfosType;
};

const initialState = {
  displayTooltip: true,
  toggleAdditionalInfos: false,
  additionalInfos: {
    label: null,
    description: null,
    isFolder: false,
    matchingEntries: null,
    dateRange: null,
    infos: null,
  },
};

const setAdditionalInfos = (state, action) => {
  if (state.toggleAdditionalInfos)
    return {
      ...state,
    };

  return {
    ...state,
    additionalInfos: (action.payload && action.payload.additionalInfos) || {
      label: null,
      description: null,
      isFolder: false,
      matchingEntries: null,
      dateRange: null,
      infos: null,
    },
  };
};

const tooltip = (
  state: TooltipStateT = initialState,
  action: Object,
): TooltipStateT => {
  switch (action.type) {
    case DISPLAY_ADDITIONAL_INFOS:
      return setAdditionalInfos(state, action);
    case TOGGLE_ADDITIONAL_INFOS:
      return {
        ...state,
        toggleAdditionalInfos: !state.toggleAdditionalInfos,
      };
    case TOGGLE_DISPLAY_TOOLTIP:
      return {
        ...state,
        displayTooltip: !state.displayTooltip,
      };
    default:
      return state;
  }
};

export default tooltip;
