import { TabT } from "../pane/types";
import FormsTab from "./FormsTab";
import { FormsStateT } from "./reducer";

export const tabDescription = {
  key: "externalForms",
  label: "rightPane.externalForms",
};

export type ExternalFormsStateT = FormsStateT | null;

const tab: TabT = {
  ...tabDescription,
  reducer: () => null, // Will be set when forms are loaded
  component: FormsTab,
};

export default tab;
