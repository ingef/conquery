import type { TabT } from "../pane/types";

import TimebasedQueryEditorTab from "./TimebasedQueryEditorTab";

const Tab: TabT = {
  key: "timebasedQueryEditor",
  labelKey: "rightPane.timebasedQueryEditor",
  tooltipKey: "help.tabTimebasedEditor",
  component: TimebasedQueryEditorTab,
};

export default Tab;
