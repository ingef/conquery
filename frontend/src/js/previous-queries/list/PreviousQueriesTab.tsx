import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import EmptyList from "../../list/EmptyList";
import { canUploadResult } from "../../user/selectors";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import { toggleFoldersOpen } from "../folderFilter/actions";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import UploadQueryResults from "../upload/UploadQueryResults";

import PreviousQueries from "./PreviousQueries";
import PreviousQueriesFolderButton from "./PreviousQueriesFolderButton";
import PreviousQueriesFolders from "./PreviousQueriesFolders";
import { useLoadPreviousQueries } from "./actions";
import { PreviousQueryT } from "./reducer";
import { selectPreviousQueries } from "./selector";

const ScrollContainer = styled("div")`
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  font-size: ${({ theme }) => theme.font.sm};
`;

const Row = styled("div")`
  display: flex;
  align-items: flex-start;
  padding: 0 10px;
`;
const FoldersAndQueries = styled(Row)`
  flex-grow: 1;
  margin-top: 8px;
  overflow: hidden;
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

const Expand = styled("div")`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
`;

interface PropsT {
  datasetId: DatasetIdT | null;
}

const PreviousQueryEditorTab = ({ datasetId }: PropsT) => {
  const { t } = useTranslation();
  const allQueries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const search = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch,
  );
  const filter = useSelector<StateT, string>(
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
    search,
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

  const loadPreviousQueries = useLoadPreviousQueries();

  const hasQueries = loading || queries.length !== 0;

  useEffect(() => {
    if (datasetId) {
      loadPreviousQueries(datasetId);
    }
  }, [datasetId]);

  const areFoldersOpen = useSelector<StateT, boolean>(
    (state) => state.previousQueriesFolderFilter.areFoldersOpen,
  );

  const dispatch = useDispatch();
  const onToggleFoldersOpen = () => dispatch(toggleFoldersOpen());

  return (
    <>
      <Row>
        {hasPermissionToUpload && (
          <PreviousQueriesFolderButton
            active={areFoldersOpen}
            onClick={onToggleFoldersOpen}
          />
        )}
        <SxPreviousQueriesSearchBox />
        {hasPermissionToUpload && (
          <SxUploadQueryResults datasetId={datasetId} />
        )}
      </Row>
      <FoldersAndQueries>
        <PreviousQueriesFolders isOpen={areFoldersOpen} />
        <Expand>
          <SxPreviousQueriesFilter />
          <ScrollContainer>
            {queries.length === 0 && !loading && (
              <EmptyList emptyMessage={t("previousQueries.noQueriesFound")} />
            )}
          </ScrollContainer>
          {hasQueries && (
            <PreviousQueries queries={queries} datasetId={datasetId} />
          )}
        </Expand>
      </FoldersAndQueries>
    </>
  );
};

export default PreviousQueryEditorTab;
