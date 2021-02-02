import React, { useEffect } from "react";
import { useStore } from "react-redux";
import StandardQueryEditorTab from "../standard-query-editor";
import TimebasedQueryEditorTab from "../timebased-query-editor";
import type { TabPropsType } from "../pane";
import { updateReducers } from "../store";
import { useGetForms } from "../api/api";

import buildExternalFormsReducer from "./reducer";

import FormsNavigation from "./FormsNavigation";
import FormsContainer from "./FormsContainer";
import FormsQueryRunner from "./FormsQueryRunner";
import { tabDescription } from ".";

const FormsTab = (props: TabPropsType) => {
  const store = useStore();
  const getForms = useGetForms();

  useEffect(() => {
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
          reducer: externalFormsReducer,
        },
      ];

      updateReducers(store, tabs);
    }

    if (props.selectedDatasetId) {
      loadForms();
    }
  }, [store, props.selectedDatasetId]);

  return (
    <>
      <FormsNavigation />
      <FormsContainer datasetId={props.selectedDatasetId} />
      <FormsQueryRunner datasetId={props.selectedDatasetId} />
    </>
  );
};

export default FormsTab;
