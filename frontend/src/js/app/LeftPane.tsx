import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import { useAreTreesAvailable } from "../concept-trees/selectors";
import Pane from "../pane/Pane";
import ProjectItemsTab from "../previous-queries/list/ProjectItemsTab";

import { StateT } from "./reducers";

const SxConceptTreeSearchBox = styled(ConceptTreeSearchBox)`
  margin: 8px 10px 5px;
`;

const LeftPane = () => {
  const { t } = useTranslation();
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.left.activeTab,
  );
  const selectedDatasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const areTreesAvailable = useAreTreesAvailable();

  // TODO: Re-implement
  // const previousQueriesLoading = useSelector<StateT, boolean>(
  //   (state) => state.previousQueries.loading,
  // );

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
          // TODO: Re-implement
          // loading: previousQueriesLoading,
        },
      ]}
    >
      {activeTab === "conceptTrees" && areTreesAvailable && (
        <SxConceptTreeSearchBox />
      )}
      <ConceptTreeList datasetId={selectedDatasetId} />
      {activeTab === "previousQueries" && (
        <ProjectItemsTab datasetId={selectedDatasetId} />
      )}
    </Pane>
  );
};

export default LeftPane;
