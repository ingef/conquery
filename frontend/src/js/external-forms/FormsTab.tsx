import React from "react";
import { useStore } from "react-redux";
import { getForms } from "../api/api";
import StandardQueryEditorTab from "../standard-query-editor";
import TimebasedQueryEditorTab from "../timebased-query-editor";
import type { TabPropsType } from "../pane";
import { updateReducers } from "../store";

import buildExternalFormsReducer from "./reducer";

import FormsNavigation from "./FormsNavigation";
import FormsContainer from "./FormsContainer";
import FormsQueryRunner from "./FormsQueryRunner";
import FormConfigSaver from "./FormConfigSaver";
import { tabDescription } from ".";

const FormsTab = (props: TabPropsType) => {
  const store = useStore();

  React.useEffect(() => {
    async function loadForms() {
      const configuredForms = await getForms(props.selectedDatasetId);

      const forms = configuredForms.reduce((all, form) => {
        all[form.type] = form;

        return all;
      }, {});

      const externalFormsReducer = buildExternalFormsReducer(forms);

      const tabs = [
        StandardQueryEditorTab,
        TimebasedQueryEditorTab,
        {
          ...tabDescription,
          reducer: externalFormsReducer
        }
      ];

      updateReducers(store, tabs);
    }

    loadForms();
  }, [store, props.selectedDatasetId]);

  return (
    <>
      <FormsNavigation />
      <FormConfigSaver datasetId={props.selectedDatasetId} />
      <FormsContainer datasetId={props.selectedDatasetId} />
      <FormsQueryRunner datasetId={props.selectedDatasetId} />
    </>
  );
};

export default FormsTab;
