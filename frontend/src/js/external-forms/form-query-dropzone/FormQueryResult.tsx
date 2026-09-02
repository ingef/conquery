import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { tv } from "tailwind-variants";

import IconButton from "../../button/IconButton";
import { exists } from "../../common/helpers/exists";
import type { DragItemQuery } from "../../standard-query-editor/types";

const root = tv({
  base: [
    "px-[10px] py-[5px]",
    "bg-white",
    "rounded",
    "text-base",
    "text-gray-800",
  ],
  variants: {
    error: {
      true: "border border-red",
      false: "border border-gray-100",
    },
  },
});

const errorMessage = tv({ base: ["text-red", "font-normal"] });

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
  return (
    <div className={root({ error: exists(error), className })}>
      {error ? (
        <span className={errorMessage()}>{error}</span>
      ) : queryResult ? (
        queryResult.label || queryResult.id
      ) : null}
      {onDelete && <IconButton tiny icon={faTimes} onClick={onDelete} />}
    </div>
  );
};

export default FormQueryResult;
