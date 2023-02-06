import { TabT } from "../pane/types";

import FormsTab from "./FormsTab";

export const tabDescription = {
  key: "externalForms",
};

const Tab: TabT = {
  ...tabDescription,
  labelKey: "rightPane.externalForms",
  tooltipKey: "help.tabFormEditor",
  component: FormsTab,
};

export default Tab;
