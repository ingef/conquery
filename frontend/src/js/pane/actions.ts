import { createAction, ActionType } from "typesafe-actions";

export type PaneActions = ActionType<typeof clickPaneTab>;

export const clickPaneTab = createAction("pane/CLICK_PANE_TAB")<{
  paneType: "left" | "right";
  tab: string;
}>();
