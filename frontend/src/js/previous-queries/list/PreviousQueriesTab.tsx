import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import EmptyList from "../../list/EmptyList";
import Loading from "../../list/Loading";
import { canUploadResult } from "../../user/selectors";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import UploadQueryResults from "../upload/UploadQueryResults";

import PreviousQueries from "./PreviousQueries";
import PreviousQueriesFolderButton from "./PreviousQueriesFolderButton";
import PreviousQueriesFolders from "./PreviousQueriesFolders";
import { useLoadPreviousQueries } from "./actions";
import { PreviousQueryT } from "./reducer";
import { selectPreviousQueries } from "./selector";

const Container = styled("div")`
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
`;
const SxPreviousQueriesSearchBox = styled(PreviousQueriesSearchBox)`
  flex-grow: 1;
`;

const SxUploadQueryResults = styled(UploadQueryResults)`
  margin-right: 5px;
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
  const queries = useSelector<StateT, PreviousQueryT[]>((state) =>
    selectPreviousQueries(
      state.previousQueries.queries,
      state.previousQueriesSearch,
      state.previousQueriesFilter,
    ),
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

  const [areFoldersOpen, setAreFoldersOpen] = useState<boolean>(false);

  return (
    <>
      <PreviousQueriesFilter />
      <Row>
        {hasPermissionToUpload && (
          <PreviousQueriesFolderButton
            active={areFoldersOpen}
            onClick={() => setAreFoldersOpen(!areFoldersOpen)}
          />
        )}
        {hasPermissionToUpload && (
          <SxUploadQueryResults datasetId={datasetId} />
        )}
        <SxPreviousQueriesSearchBox />
      </Row>
      <FoldersAndQueries>
        <PreviousQueriesFolders isOpen={areFoldersOpen} />
        <Expand>
          <Container>
            {loading && <Loading message={t("previousQueries.loading")} />}
            {queries.length === 0 && !loading && (
              <EmptyList emptyMessage={t("previousQueries.noQueriesFound")} />
            )}
          </Container>
          {hasQueries && (
            <PreviousQueries queries={queries} datasetId={datasetId} />
          )}
        </Expand>
      </FoldersAndQueries>
    </>
  );
};

export default PreviousQueryEditorTab;
