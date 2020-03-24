import React, { useEffect } from "react";
import styled from "@emotion/styled";
import { useDispatch, useSelector } from "react-redux";

import type { DatasetIdT } from "../../api/types";
import DeletePreviousQueryModal from "../delete-modal/DeletePreviousQueryModal";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import PreviousQueries from "./PreviousQueries";
import UploadQueryResults from "../upload/UploadQueryResults";

import EmptyList from "./EmptyList";
import PreviousQueriesLoading from "./PreviousQueriesLoading";

import { loadPreviousQueries } from "./actions";
import { selectPreviousQueries } from "./selector";
import { canUploadResult } from "../../user/selectors";

const Container = styled("div")`
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;

type PropsT = {
  datasetId: DatasetIdT;
};

const PreviousQueryEditorTab = ({ datasetId }: PropsT) => {
  const queries = useSelector(state =>
    selectPreviousQueries(
      state.previousQueries.queries,
      state.previousQueriesSearch,
      state.previousQueriesFilter
    )
  );
  const loading = useSelector(state => state.previousQueries.loading);
  const hasPermissionToUpload = useSelector(state => canUploadResult(state));

  const dispatch = useDispatch();

  const hasQueries = loading || queries.length !== 0;

  useEffect(() => {
    const loadQueries = () => dispatch(loadPreviousQueries(datasetId));

    loadQueries();
  }, [dispatch, datasetId]);

  return (
    <>
      <PreviousQueriesFilter />
      <PreviousQueriesSearchBox />
      {hasPermissionToUpload && <UploadQueryResults datasetId={datasetId} />}
      <Container>
        {loading && <PreviousQueriesLoading />}
        {queries.length === 0 && !loading && <EmptyList />}
      </Container>
      {hasQueries && (
        <>
          <PreviousQueries queries={queries} datasetId={datasetId} />
          <DeletePreviousQueryModal datasetId={datasetId} />
        </>
      )}
    </>
  );
};

export default PreviousQueryEditorTab;
