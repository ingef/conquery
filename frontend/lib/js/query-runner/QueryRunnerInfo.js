// @flow

import React from "react";
import classnames from "classnames";
import T from "i18n-react";
import type { StateType } from "./reducer";

type PropsType = {
  queryRunner: StateType
};

const getMessage = (queryRunner: StateType) => {
  if (queryRunner.startQuery.error)
    return { type: "error", value: T.translate("queryRunner.startError") };
  else if (queryRunner.stopQuery.error)
    return { type: "error", value: T.translate("queryRunner.stopError") };
  else if (queryRunner.queryResult && queryRunner.queryResult.error)
    return { type: "error", value: T.translate("queryRunner.resultError") };
  else if (queryRunner.startQuery.success)
    return { type: "success", value: T.translate("queryRunner.startSuccess") };
  else if (queryRunner.stopQuery.success)
    return { type: "success", value: T.translate("queryRunner.stopSuccess") };

  return null;
};

const QueryRunnerInfo = ({ queryRunner }: PropsType) => {
  const message = getMessage(queryRunner);

  return message && !queryRunner.queryResult.resultUrl ? (
    <div className="query-runner__info">
      <p
        className={classnames(
          "query-runner__status",
          `query-runner__status--${message.type}`
        )}
      >
        {message.value}
      </p>
    </div>
  ) : null;
};

export default QueryRunnerInfo;
