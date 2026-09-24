import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import { C2 } from "../ui-components/Typography";
import type { QueryRunnerStateT } from "./reducer";

const status = tv({ base: "text-right" });

const useMessage = (queryRunner: QueryRunnerStateT) => {
  const { t } = useTranslation();

  const error = queryRunner.startQuery.error;
  if (error) {
    // Maybe use type guard here
    if (typeof error === "string" && error.trim().length > 0)
      return { type: "error", value: error };
    return { type: "error", value: t("queryRunner.startError") };
  } else if (queryRunner.stopQuery.error) {
    return { type: "error", value: t("queryRunner.stopError") };
  } else if (queryRunner.queryResult?.error) {
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

const QueryRunnerInfo = ({
  queryRunner,
  className,
}: {
  className?: string;
  queryRunner: QueryRunnerStateT;
}) => {
  const message = useMessage(queryRunner);

  const { queryResult } = queryRunner;

  const noQueryResultOrError =
    !queryResult || (!!queryResult && queryResult.error);

  if (!message || !noQueryResultOrError) {
    return null;
  }

  return (
    <div className={status({ className })}>
      <C2 tone={message.type === "error" ? "danger" : "success"}>
        {message.value}
      </C2>
    </div>
  );
};

export default QueryRunnerInfo;
