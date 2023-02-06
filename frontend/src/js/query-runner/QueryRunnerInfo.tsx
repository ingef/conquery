import styled from "@emotion/styled";
import { FC } from "react";
import { useTranslation } from "react-i18next";

import type { QueryRunnerStateT } from "./reducer";

const Status = styled("p")<{ success?: boolean; error?: boolean }>`
  font-weight: 400;
  margin: 0 10px;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ theme, success, error }) =>
    success ? theme.col.green : error ? theme.col.red : "initial"};
`;

interface PropsT {
  className?: string;
  queryRunner: QueryRunnerStateT;
}

const useMessage = (queryRunner: QueryRunnerStateT) => {
  const { t } = useTranslation();

  if (queryRunner.startQuery.error) {
    return { type: "error", value: t("queryRunner.startError") };
  } else if (queryRunner.stopQuery.error) {
    return { type: "error", value: t("queryRunner.stopError") };
  } else if (!!queryRunner.queryResult && queryRunner.queryResult.error) {
    return {
      type: "error",
      value: queryRunner.queryResult.error,
    };
  } else if (queryRunner.startQuery.success) {
    return { type: "success", value: t("queryRunner.startSuccess") };
  } else if (queryRunner.stopQuery.success) {
    return { type: "success", value: t("queryRunner.stopSuccess") };
  }

  return null;
};

const QueryRunnerInfo: FC<PropsT> = ({ queryRunner, className }) => {
  const message = useMessage(queryRunner);

  const { queryResult } = queryRunner;

  const noQueryResultOrError =
    !queryResult || (!!queryResult && queryResult.error);

  if (!message || !noQueryResultOrError) {
    return null;
  }

  return (
    <Status
      className={className}
      success={message.type === "success"}
      error={message.type === "error"}
    >
      {message.value}
    </Status>
  );
};

export default QueryRunnerInfo;
