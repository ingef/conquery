import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import type { StateType } from "./reducer";

const Status = styled("p")`
  font-weight: 400;
  margin: 0 10px;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme, success, error }) =>
    success ? theme.col.green : error ? theme.col.red : "initial"};
`;

type PropsType = {
  className?: string;
  queryRunner: StateType;
};

const getMessage = (queryRunner: StateType) => {
  if (queryRunner.startQuery.error)
    return { type: "error", value: T.translate("queryRunner.startError") };
  else if (queryRunner.stopQuery.error)
    return { type: "error", value: T.translate("queryRunner.stopError") };
  else if (!!queryRunner.queryResult && queryRunner.queryResult.error)
    return { type: "error", value: T.translate("queryRunner.resultError") };
  else if (queryRunner.startQuery.success)
    return { type: "success", value: T.translate("queryRunner.startSuccess") };
  else if (queryRunner.stopQuery.success)
    return { type: "success", value: T.translate("queryRunner.stopSuccess") };

  return null;
};

const QueryRunnerInfo = ({ queryRunner, className }: PropsType) => {
  const message = getMessage(queryRunner);

  const { queryResult } = queryRunner;

  const noQueryResultOrError =
    !queryResult || (!!queryResult && queryResult.error);

  return !!message && noQueryResultOrError ? (
    <Status
      className={className}
      success={message.type === "success"}
      error={message.type === "error"}
    >
      {message.value}
    </Status>
  ) : null;
};

export default QueryRunnerInfo;
