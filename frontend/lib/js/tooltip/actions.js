// @flow

import {
  TOGGLE_DISPLAY_TOOLTIP,
  DISPLAY_ADDITIONAL_INFOS,
  HIDE_ADDITIONAL_INFOS,
} from './actionTypes';

export const displayAdditionalInfos = (node: Object) => ({
  type: DISPLAY_ADDITIONAL_INFOS,
  payload: {
    additionalInfos: {
      label: node.label,
      description: node.description,
      matchingEntries: node.matchingEntries,
      infos: node.additionalInfos,
    }
  }
});

export const hideAdditionalInfos = () => ({ type: HIDE_ADDITIONAL_INFOS });

export const toggleDisplayTooltip = () => ({ type: TOGGLE_DISPLAY_TOOLTIP });
