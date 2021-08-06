import { css } from "@emotion/react";
import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import SplitPane from "react-split-pane";

import type { DatasetIdT } from "../../api/types";
import { usePrevious } from "../../common/helpers/usePrevious";
import EmptyList from "../../list/EmptyList";
import { canUploadResult } from "../../user/selectors";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import type { PreviousQueriesFilterStateT } from "../filter/reducer";
import { toggleFoldersOpen } from "../folderFilter/actions";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import UploadQueryResults from "../upload/UploadQueryResults";

import PreviousQueries from "./PreviousQueries";
import PreviousQueriesFolderButton from "./PreviousQueriesFolderButton";
import PreviousQueriesFolders from "./PreviousQueriesFolders";
import { useLoadQueries } from "./actions";
import type { PreviousQueryT } from "./reducer";
import { selectPreviousQueries } from "./selector";

const ScrollContainer = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  font-size: ${({ theme }) => theme.font.sm};
`;

const Row = styled("div")`
  display: flex;
  align-items: flex-start;
  margin: 0 10px;
`;
const FoldersAndQueries = styled(Row)`
  flex-grow: 1;
  margin-top: 8px;
  overflow: hidden;
  position: relative;
`;
const SxPreviousQueriesSearchBox = styled(PreviousQueriesSearchBox)`
  flex-grow: 1;
`;

const SxPreviousQueriesFilter = styled(PreviousQueriesFilter)`
  margin-top: 5px;
  display: flex;
  align-items: flex-start;
`;

const SxUploadQueryResults = styled(UploadQueryResults)`
  margin-left: 5px;
`;

const SxPreviousQueriesFolders = styled(PreviousQueriesFolders)`
  padding: 8px 8px 8px 0;
`;

const Expand = styled("div")<{ areFoldersOpen?: boolean }>`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  ${({ areFoldersOpen }) =>
    areFoldersOpen &&
    css`
      padding-left: 8px;
    `}
`;

interface PropsT {
  datasetId: DatasetIdT | null;
}

const PreviousQueryEditorTab = ({ datasetId }: PropsT) => {
  const { t } = useTranslation();
  const allQueries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const searchTerm = useSelector<StateT, string | null>(
    (state) => state.previousQueriesSearch.searchTerm,
  );
  const filter = useSelector<StateT, PreviousQueriesFilterStateT>(
    (state) => state.previousQueriesFilter,
  );
  const folders = useSelector<StateT, string[]>(
    (state) => state.previousQueriesFolderFilter.folders,
  );
  const noFoldersActive = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.noFoldersActive,
  );
  const queries = selectPreviousQueries(
    allQueries,
    searchTerm,
    filter,
    folders,
    noFoldersActive,
  );

  const loading = useSelector<StateT, boolean>(
    (state) => state.previousQueries.loading,
  );
  const hasPermissionToUpload = useSelector<StateT, boolean>((state) =>
    canUploadResult(state),
  );

  const loadQueries = useLoadQueries();

  useEffect(() => {
    if (datasetId) {
      loadQueries(datasetId);
    }
  }, [datasetId]);

  const areFoldersOpen = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.areFoldersOpen,
  );

  const wereFoldersOpen = usePrevious(areFoldersOpen);

  const dispatch = useDispatch();
  const onToggleFoldersOpen = () => dispatch(toggleFoldersOpen());

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

  return (
    <>
      <Row>
        <PreviousQueriesFolderButton
          active={areFoldersOpen}
          onClick={onToggleFoldersOpen}
        />
        <SxPreviousQueriesSearchBox />
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
          <SxPreviousQueriesFolders />
          <Expand areFoldersOpen={areFoldersOpen}>
            <SxPreviousQueriesFilter />
            <ScrollContainer>
              {queries.length === 0 && !loading && (
                <EmptyList emptyMessage={t("previousQueries.noQueriesFound")} />
              )}
            </ScrollContainer>
            <PreviousQueries queries={queries} datasetId={datasetId} />
          </Expand>
        </SplitPane>
      </FoldersAndQueries>
    </>
  );
};

export default PreviousQueryEditorTab;
