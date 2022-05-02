import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";

import { useGetQuery } from "../../api/api";
import { useDatasetId } from "../../dataset/selectors";
import type { DragItemQuery } from "../../standard-query-editor/types";

import FormQueryResult from "./FormQueryResult";

interface PropsT {
  queryResult?: DragItemQuery;
  className?: string;
  onDelete?: () => void;
  onInvalid: (error: string) => void;
}

const ValidatedFormQueryResult = ({ onInvalid, ...props }: PropsT) => {
  const datasetId = useDatasetId();
  const getQuery = useGetQuery();
  const { t } = useTranslation();

  const [localError, setLocalError] = useState(false);

  useEffect(
    function validateQuery() {
      const loadAndValidateQuery = async () => {
        if (datasetId && props.queryResult) {
          try {
            await getQuery(datasetId, props.queryResult.id);
            setLocalError(false);
          } catch (e) {
            setLocalError(true);
            onInvalid(t("previousQuery.loadError"));
          }
        }
      };

      loadAndValidateQuery();
    },
    [datasetId, props.queryResult],
  );

  return (
    <FormQueryResult
      {...props}
      error={localError ? t("previousQuery.loadError") : undefined}
    />
  );
};

export default ValidatedFormQueryResult;
