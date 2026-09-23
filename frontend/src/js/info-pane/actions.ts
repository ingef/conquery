import { type ActionType, createAction } from "typesafe-actions";

import type { AdditionalInfosType } from "./reducer";

export type InfoPaneActions = ActionType<
  | typeof displayAdditionalInfos
  | typeof toggleAdditionalInfos
  | typeof toggleInfoPane
  | typeof collapseInfoPane
  | typeof expandInfoPane
>;

export const displayAdditionalInfos = createAction(
  "infoPane/DISPLAY_ADDITIONAL_INFOS",
)<{ additionalInfos: AdditionalInfosType }>();

export const toggleAdditionalInfos = createAction(
  "infoPane/TOGGLE_ADDITIONAL_INFOS",
)();
export const toggleInfoPane = createAction("infoPane/TOGGLE")();
// the panel collapsed on its own: dragged under its minimum, or the window got too narrow
export const collapseInfoPane = createAction("infoPane/COLLAPSE")<{
  byLayout: boolean;
}>();
export const expandInfoPane = createAction("infoPane/EXPAND")();
