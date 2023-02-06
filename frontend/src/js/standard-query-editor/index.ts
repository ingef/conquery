import type { TabT } from "../pane/types";

import StandardQueryEditorTab from "./StandardQueryEditorTab";

const Tab: TabT = {
  key: "queryEditor",
  labelKey: "rightPane.queryEditor",
  tooltipKey: "help.tabQueryEditor",
  component: StandardQueryEditorTab,
};

export default Tab;
