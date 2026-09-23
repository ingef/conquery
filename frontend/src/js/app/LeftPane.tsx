import { ChevronRightIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetT } from "../api/types";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import { useAreTreesAvailable } from "../concept-trees/selectors";
import { toggleInfoPane } from "../info-pane/actions";
import Pane from "../pane/Pane";
import ProjectItemsTab from "../previous-queries/list/ProjectItemsTab";
import { Button } from "../ui-components/Button";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import type { StateT } from "./reducers";

const LeftPane = () => {
  const { t } = useTranslation();
  const selectedDatasetId = useSelector<StateT, DatasetT["id"] | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const areTreesAvailable = useAreTreesAvailable();
  const isInfoPaneOpen = useSelector<StateT, boolean>(
    (state) => state.infoPane.isOpen,
  );
  const dispatch = useDispatch();

  // TODO: Re-implement
  // const previousQueriesLoading = useSelector<StateT, boolean>(
  //   (state) => state.previousQueries.loading,
  // );

  return (
    <Pane
      left
      beforeTabs={
        !isInfoPaneOpen && (
          <TooltipTrigger>
            <Button
              intent="tertiary"
              aria-label={t("infoPane.expand")}
              onPress={() => dispatch(toggleInfoPane())}
            >
              <ChevronRightIcon />
            </Button>
            <Tooltip>{t("infoPane.expand")}</Tooltip>
          </TooltipTrigger>
        )
      }
      tabs={[
        {
          label: t("leftPane.conceptTrees"),
          key: "conceptTrees",
          tooltip: t("help.tabConceptTrees"),
          // the list takes the height left over by the search box
          content: (
            <div className="flex flex-col">
              {areTreesAvailable && (
                <ConceptTreeSearchBox className="mx-[10px] mt-2 mb-[5px]" />
              )}
              <ConceptTreeList datasetId={selectedDatasetId} />
            </div>
          ),
        },
        {
          label: t("leftPane.previousQueries"),
          key: "previousQueries",
          tooltip: t("help.tabPreviousQueries"),
          content: <ProjectItemsTab datasetId={selectedDatasetId} />,
        },
      ]}
      dataTestId="left-pane"
    />
  );
};

export default LeftPane;
