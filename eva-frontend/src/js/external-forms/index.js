// @flow

import psmForm from "../forms/psm-form";
import exportForm from "../forms/export-form";
import mapForm from "../forms/map-form";
import descriptionForm from "../forms/description-form";
import auForm from "../forms/au-form";

import buildExternalFormsReducer from "./reducer";
import ExternalFormsTab from "./ExternalFormsTab";

const forms = {
  [psmForm.type]: psmForm,
  [exportForm.type]: exportForm,
  [mapForm.type]: mapForm,
  [descriptionForm.type]: descriptionForm,
  [auForm.type]: auForm
};

const externalFormsReducer = buildExternalFormsReducer(forms);

export default {
  key: "externalForms",
  label: "rightPane.externalForms",
  reducer: externalFormsReducer,
  component: ExternalFormsTab
};
