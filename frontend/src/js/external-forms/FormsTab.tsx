import React, { useEffect } from "react";
import { StateT } from "app-types";
import { useSelector, useStore } from "react-redux";

import StandardQueryEditorTab from "../standard-query-editor";
import TimebasedQueryEditorTab from "../timebased-query-editor";
import { updateReducers } from "../store";
import { useGetForms } from "../api/api";
import { DatasetIdT } from "../api/types";

import buildExternalFormsReducer from "./reducer";

import FormsNavigation from "./FormsNavigation";
import FormsContainer from "./FormsContainer";
import FormsQueryRunner from "./FormsQueryRunner";
import { tabDescription } from ".";

const FormsTab = () => {
  const store = useStore();
  const getForms = useGetForms();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );

  useEffect(() => {
    async function loadForms() {
      if (!datasetId) {
        return;
      }

      const configuredForms = await getForms(datasetId);

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

    loadForms();
  }, [store, datasetId]);

  return (
    <>
      <FormsNavigation />
      <FormsContainer />
      <FormsQueryRunner />
    </>
  );
};

export default FormsTab;
