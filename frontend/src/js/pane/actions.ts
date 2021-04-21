import { CLICK_PANE_TAB } from "./actionTypes";

export const clickPaneTab = (paneType: "left" | "right", tab: string) => ({
  type: CLICK_PANE_TAB,
  payload: { paneType, tab },
});
