// @flow

import React from "react";

import type { TabPropsType } from "conquery/lib/js/pane";
import ExternalFormsNavigation from "./ExternalFormsNavigation";
import ExternalFormsContainer from "./ExternalFormsContainer";
import ExternalFormsQueryRunner from "./ExternalFormsQueryRunner";

const ExternalFormsTab = (props: TabPropsType) => (
  <>
    <ExternalFormsNavigation />
    <ExternalFormsContainer datasetId={props.selectedDatasetId} />
    <ExternalFormsQueryRunner datasetId={props.selectedDatasetId} />
  </>
);

export default ExternalFormsTab;
