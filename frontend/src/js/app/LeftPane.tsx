import React from "react";
import styled from "@emotion/styled";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";

import Pane from "../pane/Pane";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import PreviousQueriesTab from "../previous-queries/list/PreviousQueriesTab";
import FormConfigsTab from "../external-forms/form-configs/FormConfigsTab";
import { StateT } from "./reducers";

import { getAreTreesAvailable } from "../concept-trees/selectors";
import { useTranslation } from "react-i18next";

const SxConceptTreeSearchBox = styled(ConceptTreeSearchBox)`
  margin: 0 10px 5px;
`;

const LeftPane = () => {
  const { t } = useTranslation();
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.left.activeTab
  );
  const selectedDatasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId
  );
  const areTreesAvailable = useSelector<StateT, boolean>((state) =>
    getAreTreesAvailable(state)
  );

  return (
    <Pane
      left
      tabs={[
        {
          label: t("leftPane.conceptTrees"),
          key: "conceptTrees",
          tooltip: t("help.tabConceptTrees"),
        },
        {
          label: t("leftPane.previousQueries"),
          key: "previousQueries",
          tooltip: t("help.tabPreviousQueries"),
        },
        {
          label: t("leftPane.formConfigs"),
          key: "formConfigs",
          tooltip: t("help.tabFormConfigs"),
        },
      ]}
    >
      {activeTab === "conceptTrees" &&
        areTreesAvailable &&
        selectedDatasetId && (
          <SxConceptTreeSearchBox datasetId={selectedDatasetId} />
        )}
      <ConceptTreeList datasetId={selectedDatasetId} />
      {activeTab === "previousQueries" && (
        <PreviousQueriesTab datasetId={selectedDatasetId} />
      )}
      {activeTab === "formConfigs" && (
        <FormConfigsTab datasetId={selectedDatasetId} />
      )}
    </Pane>
  );
};

export default LeftPane;
