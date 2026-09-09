import { type ActionType, createAction } from "typesafe-actions";

import type { AdditionalInfosType } from "./reducer";

export type InfoPaneActions = ActionType<
  | typeof displayAdditionalInfos
  | typeof toggleAdditionalInfos
  | typeof toggleInfoPane
>;

export const displayAdditionalInfos = createAction(
  "infoPane/DISPLAY_ADDITIONAL_INFOS",
)<{ additionalInfos: AdditionalInfosType }>();

export const toggleAdditionalInfos = createAction(
  "infoPane/TOGGLE_ADDITIONAL_INFOS",
)();
export const toggleInfoPane = createAction("infoPane/TOGGLE")();
