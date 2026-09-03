import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetT } from "../api/types";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import { useAreTreesAvailable } from "../concept-trees/selectors";
import Pane from "../pane/Pane";
import ProjectItemsTab from "../previous-queries/list/ProjectItemsTab";

import type { StateT } from "./reducers";

const LeftPane = () => {
  const { t } = useTranslation();
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.left.activeTab,
  );
  const selectedDatasetId = useSelector<StateT, DatasetT["id"] | null>(
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
      dataTestId="left-pane"
    >
      {activeTab === "conceptTrees" && areTreesAvailable && (
        <ConceptTreeSearchBox className="mx-[10px] mt-2 mb-[5px]" />
      )}
      <ConceptTreeList datasetId={selectedDatasetId} />
      {activeTab === "previousQueries" && (
        <ProjectItemsTab datasetId={selectedDatasetId} />
      )}
    </Pane>
  );
};

export default LeftPane;
