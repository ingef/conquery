import { XIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { exists } from "../../common/helpers/exists";
import type { DragItemQuery } from "../../standard-query-editor/types";
import { Button } from "../../ui-components/Button";
import { C2 } from "../../ui-components/Typography";

const root = tv({
  base: ["px-[10px] py-[5px]", "bg-white", "rounded"],
  variants: {
    error: {
      true: "border border-red",
      false: "border border-gray-100",
    },
  },
});

interface PropsT {
  queryResult?: DragItemQuery;
  className?: string;
  error?: string;
  onDelete?: () => void;
}

const FormQueryResult = ({
  queryResult,
  className,
  error,
  onDelete,
}: PropsT) => {
  const { t } = useTranslation();
  return (
    <div className={root({ error: exists(error), className })}>
      {error ? (
        <C2 as="span" tone="danger">
          {error}
        </C2>
      ) : queryResult ? (
        queryResult.label || queryResult.id
      ) : null}
      {onDelete && (
        <Button
          intent="tertiary"
          size="sm"
          aria-label={t("common.delete")}
          onPress={onDelete}
        >
          <XIcon />
        </Button>
      )}
    </div>
  );
};

export default FormQueryResult;
