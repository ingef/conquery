import { useEffect, useState } from "react";

import { useGetQuery } from "../../api/api";
import { useDatasetId } from "../../dataset/selectors";
import type { DragItemQuery } from "../../standard-query-editor/types";

import FormQueryResult from "./FormQueryResult";

interface PropsT {
  queryResult?: DragItemQuery;
  className?: string;
  onDelete?: () => void;
  onInvalid: () => void;
}

const ValidatedFormQueryResult = ({ onInvalid, ...props }: PropsT) => {
  const datasetId = useDatasetId();
  const getQuery = useGetQuery();
  const [error, setError] = useState(false);

  useEffect(
    function validateQuery() {
      const loadAndValidateQuery = async () => {
        if (datasetId && props.queryResult) {
          try {
            await getQuery(datasetId, props.queryResult.id);
            setError(false);
          } catch (e) {
            setError(true);
            onInvalid();
          }
        }
      };

      loadAndValidateQuery();
    },
    [datasetId, props.queryResult],
  );

  return <FormQueryResult {...props} error={error} />;
};

export default ValidatedFormQueryResult;
