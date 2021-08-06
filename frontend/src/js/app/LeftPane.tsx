import styled from "@emotion/styled";
import React from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import { getAreTreesAvailable } from "../concept-trees/selectors";
import FormConfigsTab from "../external-forms/form-configs/FormConfigsTab";
import Pane from "../pane/Pane";
import PreviousQueriesTab from "../previous-queries/list/PreviousQueriesTab";

import { StateT } from "./reducers";

const SxConceptTreeSearchBox = styled(ConceptTreeSearchBox)`
  margin: 0 10px 5px;
`;

const LeftPane = () => {
  const { t } = useTranslation();
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.left.activeTab,
  );
  const selectedDatasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const areTreesAvailable = useSelector<StateT, boolean>((state) =>
    getAreTreesAvailable(state),
  );
  const previousQueriesLoading = useSelector<StateT, boolean>(
    (state) => state.previousQueries.loading,
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
          loading: previousQueriesLoading,
        },
        {
          label: t("leftPane.formConfigs"),
          key: "formConfigs",
          tooltip: t("help.tabFormConfigs"),
        },
      ]}
    >
      {activeTab === "conceptTrees" && areTreesAvailable && (
        <SxConceptTreeSearchBox />
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
