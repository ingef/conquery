import { ActionType, createAction } from "typesafe-actions";

export type PaneActions =
  | ActionType<typeof clickPaneTab>
  | ActionType<typeof toggleDragHandles>;

export const clickPaneTab = createAction("pane/CLICK_PANE_TAB")<{
  paneType: "left" | "right";
  tab: string;
}>();

export const toggleDragHandles = createAction("pane/TOGGLE_DRAG_HANDLES")();
