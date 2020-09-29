import FormsTab from "./FormsTab";
import { FormsStateT } from "./reducer";

export const tabDescription = {
  key: "externalForms",
  label: "rightPane.externalForms"
};

export type ExternalFormsStateT = FormsStateT | null;

export default {
  ...tabDescription,
  reducer: {}, // Will be set when forms are loaded
  component: FormsTab
};
