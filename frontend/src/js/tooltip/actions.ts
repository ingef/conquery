import {
  TOGGLE_DISPLAY_TOOLTIP,
  DISPLAY_ADDITIONAL_INFOS,
  TOGGLE_ADDITIONAL_INFOS,
} from "./actionTypes";

export const displayAdditionalInfos = (additionalInfos: Object) => ({
  type: DISPLAY_ADDITIONAL_INFOS,
  payload: {
    additionalInfos,
  },
});

export const toggleAdditionalInfos = () => ({ type: TOGGLE_ADDITIONAL_INFOS });
export const toggleDisplayTooltip = () => ({ type: TOGGLE_DISPLAY_TOOLTIP });
