import { useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { useDatasetId } from "../dataset/selectors";
import { useStartQuery, useStopQuery } from "../query-runner/actions";
import QueryRunner from "../query-runner/QueryRunner";
import type { QueryRunnerStateT } from "../query-runner/reducer";

import type { EditorV2Query } from "./types";

export const EditorV2QueryRunner = ({ query }: { query: EditorV2Query }) => {
  const datasetId = useDatasetId();
  const queryRunner = useSelector<StateT, QueryRunnerStateT>(
    (state) => state.editorV2QueryRunner,
  );
  const startStandardQuery = useStartQuery("editorV2");
  const stopStandardQuery = useStopQuery("editorV2");

  const startQuery = () => {
    if (datasetId) {
      startStandardQuery(datasetId, query);
    }
  };

  const queryId = queryRunner.runningQuery;
  const stopQuery = () => {
    if (queryId) {
      stopStandardQuery(queryId);
    }
  };

  const disabled = !query.tree;

  return (
    <QueryRunner
      queryRunner={queryRunner}
      disabled={disabled}
      isQueryRunning={false}
      startQuery={startQuery}
      stopQuery={stopQuery}
    />
  );
};
