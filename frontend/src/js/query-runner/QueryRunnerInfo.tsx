import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";

import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
} from "../ui-components/Tooltip";
import type { QueryRunnerStateT } from "./reducer";

// one line in the query runner's row; the tooltip carries the full text
const status = tv({
  base: ["m-0", "truncate", "text-sm", "font-normal"],
  variants: {
    success: { true: "text-green" },
    error: { true: "text-red" },
  },
});

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
    <TooltipTrigger>
      <TooltipTarget
        as="p"
        excludeFromTabOrder
        className={status({
          success: message.type === "success",
          error: message.type === "error",
          className,
        })}
      >
        {message.value}
      </TooltipTarget>
      <Tooltip>{message.value}</Tooltip>
    </TooltipTrigger>
  );
};

export default QueryRunnerInfo;
