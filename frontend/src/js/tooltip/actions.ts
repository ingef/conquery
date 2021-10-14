import { ActionType, createAction } from "typesafe-actions";

import type { AdditionalInfosType } from "./reducer";

export type TooltipActions = ActionType<
  | typeof displayAdditionalInfos
  | typeof toggleAdditionalInfos
  | typeof toggleDisplayTooltip
>;

export const displayAdditionalInfos = createAction(
  "tooltip/DISPLAY_ADDITIONAL_INFOS",
)<{ additionalInfos: AdditionalInfosType }>();

export const toggleAdditionalInfos = createAction(
  "tooltip/TOGGLE_ADDITIONAL_INFOS",
)();
export const toggleDisplayTooltip = createAction(
  "tooltip/TOGGLE_DISPLAY_TOOLTIP",
)();
