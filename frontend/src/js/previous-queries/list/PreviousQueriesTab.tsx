import React, { useEffect } from "react";
import styled from "@emotion/styled";
import { useDispatch, useSelector } from "react-redux";
import { StateT } from "app-types";

import type { DatasetIdT } from "../../api/types";
import PreviousQueriesSearchBox from "../search/PreviousQueriesSearchBox";
import PreviousQueriesFilter from "../filter/PreviousQueriesFilter";
import PreviousQueries from "./PreviousQueries";
import UploadQueryResults from "../upload/UploadQueryResults";

import { loadPreviousQueries } from "./actions";
import { selectPreviousQueries } from "./selector";
import { canUploadResult } from "../../user/selectors";

import { T } from "../../localization";

import EmptyList from "../../list/EmptyList";
import Loading from "../../list/Loading";
import { PreviousQueryT } from "./reducer";

const Container = styled("div")`
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;

type PropsT = {
  datasetId: DatasetIdT;
};

const PreviousQueryEditorTab = ({ datasetId }: PropsT) => {
  const queries = useSelector<StateT, PreviousQueryT[]>((state) =>
    selectPreviousQueries(
      state.previousQueries.queries,
      state.previousQueriesSearch,
      state.previousQueriesFilter
    )
  );
  const loading = useSelector<StateT, boolean>(
    (state) => state.previousQueries.loading
  );
  const hasPermissionToUpload = useSelector<StateT, boolean>((state) =>
    canUploadResult(state)
  );

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
        {loading && (
          <Loading message={T.translate("previousQueries.loading")} />
        )}
        {queries.length === 0 && !loading && (
          <EmptyList
            emptyMessage={T.translate("previousQueries.noQueriesFound")}
          />
        )}
      </Container>
      {hasQueries && (
        <PreviousQueries queries={queries} datasetId={datasetId} />
      )}
    </>
  );
};

export default PreviousQueryEditorTab;
