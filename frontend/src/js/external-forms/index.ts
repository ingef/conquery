import FormsTab from "./FormsTab";

export const tabDescription = {
  key: "externalForms",
  label: "rightPane.externalForms"
};

export default {
  ...tabDescription,
  reducer: {}, // Will be set when forms are loaded
  component: FormsTab
};
