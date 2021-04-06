import { TabT } from "../pane/types";
import FormsTab from "./FormsTab";
import buildExternalFormsReducer from "./reducer";
import { FormsStateT } from "./reducer";

export const tabDescription = {
  key: "externalForms",
};

export type ExternalFormsStateT = FormsStateT | null;

const Tab: TabT = {
  ...tabDescription,
  labelKey: "rightPane.externalForms",
  tooltipKey: "help.tabFormEditor",
  reducer: buildExternalFormsReducer({}), // Will be set when forms are loaded
  component: FormsTab,
};

export default Tab;
