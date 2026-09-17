import { FolderOpenIcon } from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { Group, Panel } from "react-resizable-panels";
import { tv } from "tailwind-variants";
import type { DatasetT } from "../../api/types";
import type { StateT } from "../../app/reducers";
import { ResizeHandle } from "../../common/ResizeHandle";
import { useCollapsiblePanel } from "../../common/useCollapsiblePanel";
import { selectFormConfigs } from "../../external-forms/form-configs/selectors";
import { EmptyState } from "../../ui-components/EmptyState";
import { canUploadResult } from "../../user/selectors";
import ProjectItemsFilter from "../filter/ProjectItemsFilter";
import type { ProjectItemsFilterStateT } from "../filter/reducer";
import { toggleFoldersOpen } from "../folder-filter/actions";
import ProjectItemsSearchBox from "../search/ProjectItemsSearchBox";
import ProjectItemsTypeFilter from "../type-filter/ProjectItemsTypeFilter";
import type { ProjectItemsTypeFilterStateT } from "../type-filter/reducer";
import UploadQueryResults from "../upload/UploadQueryResults";
import { useLoadFormConfigs, useLoadQueries } from "./actions";
import Folders from "./Folders";
import FoldersToggleButton from "./FoldersToggleButton";
import type { ProjectItemT } from "./ProjectItem";
import { ProjectItems } from "./ProjectItems";
import type { FormConfigT, PreviousQueryT } from "./reducer";
import { selectPreviousQueries } from "./selector";

const FOLDERS_SHARE = "25";

const foldersAndQueries = tv({
  base: [
    "flex items-start",
    "grow",
    "mt-2 mr-2 mb-0 ml-[10px]",
    "overflow-hidden",
    "relative",
  ],
});

const expand = tv({
  base: ["flex flex-col", "grow", "h-full", "pr-[2px]"],
  variants: {
    areFoldersOpen: { true: "pl-2" },
  },
});

interface PropsT {
  datasetId: DatasetT["id"] | null;
}

const ProjectItemsTab = ({ datasetId }: PropsT) => {
  const { t } = useTranslation();
  const hasPermissionToUpload = useSelector<StateT, boolean>(canUploadResult);

  const areFoldersOpen = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.areFoldersOpen,
  );

  const dispatch = useDispatch();
  const onToggleFoldersOpen = () => dispatch(toggleFoldersOpen());

  const { items, loading } = useProjectItems({ datasetId });

  const foldersPanelRef = useCollapsiblePanel(!areFoldersOpen, FOLDERS_SHARE);
  // read once, a changing default resets the group's layout
  const [defaultSize] = useState(() => (areFoldersOpen ? FOLDERS_SHARE : 0));

  return (
    // the list takes the height left over by the search row
    <div className="flex flex-col">
      <div className="mx-[10px] mt-2 flex items-start gap-[5px]">
        <FoldersToggleButton
          active={areFoldersOpen}
          onClick={onToggleFoldersOpen}
        />
        <ProjectItemsSearchBox className="grow" />
        {hasPermissionToUpload && <UploadQueryResults datasetId={datasetId} />}
      </div>
      <div className={foldersAndQueries()}>
        <Group orientation="horizontal">
          <Panel
            key="left"
            panelRef={foldersPanelRef}
            collapsible
            collapsedSize={0}
            minSize="10"
            defaultSize={defaultSize}
          >
            <Folders className="py-2 pr-2 pl-0" />
          </Panel>
          <ResizeHandle
            disabled={!areFoldersOpen}
            style={areFoldersOpen ? undefined : { display: "none" }}
          />
          <Panel key="right">
            <div className={expand({ areFoldersOpen })}>
              <div className="my-2 flex items-start gap-5">
                <ProjectItemsTypeFilter />
                <ProjectItemsFilter />
              </div>
              {items.length === 0 && !loading ? (
                <EmptyState icon={FolderOpenIcon}>
                  {t("previousQueries.noQueriesFound")}
                </EmptyState>
              ) : (
                <ProjectItems items={items} datasetId={datasetId} />
              )}
            </div>
          </Panel>
        </Group>
      </div>
    </div>
  );
};

export default ProjectItemsTab;

interface FilterAndFetchConfig {
  datasetId: DatasetT["id"] | null;
  searchTerm: string | null;
  filter: ProjectItemsFilterStateT;
  typeFilter: ProjectItemsTypeFilterStateT;
  folders: string[];
  noFoldersActive: boolean;
}

const useProjectItems = ({
  datasetId,
}: {
  datasetId: DatasetT["id"] | null;
}) => {
  const searchTerm = useSelector<StateT, string | null>(
    (state) => state.projectItemsSearch.searchTerm,
  );
  const filter = useSelector<StateT, ProjectItemsFilterStateT>(
    (state) => state.projectItemsFilter,
  );
  const typeFilter = useSelector<StateT, ProjectItemsTypeFilterStateT>(
    (state) => state.projectItemsTypeFilter,
  );
  const folders = useSelector<StateT, string[]>(
    (state) => state.previousQueriesFolderFilter.folders,
  );
  const noFoldersActive = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.noFoldersActive,
  );

  const config: FilterAndFetchConfig = {
    datasetId,
    searchTerm,
    filter,
    typeFilter,
    folders,
    noFoldersActive,
  };

  const { queries, loading: loadingQueries } = useQueries(config);
  const { formConfigs, loading: loadingFormConfigs } = useFormConfigs(config);

  const baseItems =
    typeFilter === "queries"
      ? queries
      : typeFilter === "configs"
        ? formConfigs
        : [...queries, ...formConfigs];

  const items: ProjectItemT[] = baseItems.sort(
    (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
  );

  const loading = loadingQueries || loadingFormConfigs;

  return {
    items,
    loading,
  };
};

const useQueries = ({
  datasetId,
  searchTerm,
  filter,
  folders,
  noFoldersActive,
}: FilterAndFetchConfig) => {
  const allQueries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const queries = selectPreviousQueries(
    allQueries,
    searchTerm,
    filter,
    folders,
    noFoldersActive,
  );

  const { loading, loadQueries } = useLoadQueries();

  useEffect(() => {
    if (datasetId) {
      loadQueries(datasetId);
    }
  }, [datasetId, loadQueries]);

  return {
    queries,
    loading,
  };
};

const useFormConfigs = ({
  datasetId,
  searchTerm,
  filter,
  folders,
  noFoldersActive,
}: FilterAndFetchConfig) => {
  const allFormConfigs = useSelector<StateT, FormConfigT[]>(
    (state) => state.previousQueries.formConfigs,
  );

  const formConfigs = selectFormConfigs(
    allFormConfigs,
    searchTerm,
    filter,
    folders,
    noFoldersActive,
  );

  const { loading, loadFormConfigs } = useLoadFormConfigs();

  useEffect(() => {
    if (datasetId) {
      loadFormConfigs(datasetId);
    }
  }, [datasetId, loadFormConfigs]);

  return {
    formConfigs,
    loading,
  };
};
