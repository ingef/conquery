import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { StateT } from "app-types";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import type { DatasetIdT } from "../../api/types";
import { usePrevious } from "../../common/helpers/usePrevious";
import { selectFormConfigs } from "../../external-forms/form-configs/selectors";
import EmptyList from "../../list/EmptyList";
import { canUploadResult } from "../../user/selectors";
import ProjectItemsFilter from "../filter/ProjectItemsFilter";
import type { ProjectItemsFilterStateT } from "../filter/reducer";
import { toggleFoldersOpen } from "../folder-filter/actions";
import ProjectItemsSearchBox from "../search/ProjectItemsSearchBox";
import ProjectItemsTypeFilter from "../type-filter/ProjectItemsTypeFilter";
import { ProjectItemsTypeFilterStateT } from "../type-filter/reducer";
import UploadQueryResults from "../upload/UploadQueryResults";

import Folders from "./Folders";
import FoldersToggleButton from "./FoldersToggleButton";
import { ProjectItemT } from "./ProjectItem";
import PreviousQueries from "./ProjectItems";
import { useLoadFormConfigs, useLoadQueries } from "./actions";
import type { FormConfigT, PreviousQueryT } from "./reducer";
import { selectPreviousQueries } from "./selector";

const ScrollContainer = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  font-size: ${({ theme }) => theme.font.sm};
`;

const Row = styled("div")`
  display: flex;
  align-items: flex-start;
  margin: 8px 10px 0;
`;

const FoldersAndQueries = styled(Row)`
  flex-grow: 1;
  margin: 8px 8px 0 10px;
  overflow: hidden;
  position: relative;
`;
const SxProjectItemsSearchBox = styled(ProjectItemsSearchBox)`
  flex-grow: 1;
`;

const Filters = styled("div")`
  display: flex;
  align-items: flex-start;
`;
const SxProjectItemsFilter = styled(ProjectItemsFilter)`
  display: flex;
  align-items: flex-start;
`;

const SxProjectItemsTypeFilter = styled(ProjectItemsTypeFilter)`
  display: flex;
  align-items: flex-start;
  margin-right: 10px;
  padding-right: 10px;
`;

const SxUploadQueryResults = styled(UploadQueryResults)`
  margin-left: 5px;
`;

const SxFolders = styled(Folders)`
  padding: 8px 8px 8px 0;
`;

const Expand = styled("div")<{ areFoldersOpen?: boolean }>`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  padding-right: 2px;
  ${({ areFoldersOpen }) =>
    areFoldersOpen &&
    css`
      padding-left: 8px;
    `}
`;

interface PropsT {
  datasetId: DatasetIdT | null;
}

const ProjectItemsTab = ({ datasetId }: PropsT) => {
  const { t } = useTranslation();
  const hasPermissionToUpload = useSelector<StateT, boolean>((state) =>
    canUploadResult(state),
  );

  const areFoldersOpen = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.areFoldersOpen,
  );

  const { leftPaneSize, setLeftPaneSize } = useLeftPaneSize({ areFoldersOpen });

  const dispatch = useDispatch();
  const onToggleFoldersOpen = () => dispatch(toggleFoldersOpen());

  const { items, loading } = useProjectItems({ datasetId });

  return (
    <>
      <Row>
        <FoldersToggleButton
          active={areFoldersOpen}
          onClick={onToggleFoldersOpen}
        />
        <SxProjectItemsSearchBox />
        {hasPermissionToUpload && (
          <SxUploadQueryResults datasetId={datasetId} />
        )}
      </Row>
      <FoldersAndQueries>
        <SplitPane
          split="vertical"
          allowResize={true}
          minSize={100}
          size={leftPaneSize}
          maxSize={600}
          defaultSize={"25%"}
          onDragFinished={(newSize) => setLeftPaneSize(newSize)}
          resizerStyle={{
            zIndex: 0, // To set below overlaying dropdowns
            marginTop: "35px",
            display: areFoldersOpen ? "inherit" : "none",
          }}
        >
          <SxFolders />
          <Expand areFoldersOpen={areFoldersOpen}>
            <Filters>
              <SxProjectItemsTypeFilter />
              <SxProjectItemsFilter />
            </Filters>
            <ScrollContainer>
              {items.length === 0 && !loading && (
                <EmptyList emptyMessage={t("previousQueries.noQueriesFound")} />
              )}
            </ScrollContainer>
            <PreviousQueries items={items} datasetId={datasetId} />
          </Expand>
        </SplitPane>
      </FoldersAndQueries>
    </>
  );
};

export default ProjectItemsTab;

const useLeftPaneSize = ({ areFoldersOpen }: { areFoldersOpen?: boolean }) => {
  const wereFoldersOpen = usePrevious(areFoldersOpen);

  const [leftPaneSize, setLeftPaneSize] = useState<number | string>(0);
  const [storedPaneSize, setStoredPaneSize] = useState<number | string>(0);

  useEffect(() => {
    if (areFoldersOpen === wereFoldersOpen) {
      return;
    }

    if (!areFoldersOpen) {
      setStoredPaneSize(leftPaneSize);
      setLeftPaneSize(0);
    } else {
      setLeftPaneSize(storedPaneSize || "25%");
    }
  }, [leftPaneSize, storedPaneSize, areFoldersOpen, wereFoldersOpen]);

  return {
    leftPaneSize,
    setLeftPaneSize,
  };
};

interface FilterAndFetchConfig {
  datasetId: DatasetIdT | null;
  searchTerm: string | null;
  filter: ProjectItemsFilterStateT;
  typeFilter: ProjectItemsTypeFilterStateT;
  folders: string[];
  noFoldersActive: boolean;
}

const useProjectItems = ({ datasetId }: { datasetId: DatasetIdT | null }) => {
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
  }, [datasetId]);

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

  // TODO: Implement
  // const activeFormType = useActiveFormType();

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
  }, [datasetId]);

  return {
    formConfigs,
    loading,
  };
};
