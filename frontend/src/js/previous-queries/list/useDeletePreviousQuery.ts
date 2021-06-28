import { StateT } from "app-types";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import { useDeleteStoredQuery } from "../../api/api";
import type { DatasetIdT } from "../../api/types";
import { setMessage } from "../../snack-message/actions";

import { deletePreviousQuerySuccess } from "./actions";
import type { PreviousQueryIdT } from "./reducer";

export const useDeletePreviousQuery = (
  previousQueryId: PreviousQueryIdT,
  onSuccess?: () => void,
) => {
  const { t } = useTranslation();
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const dispatch = useDispatch();
  const deleteStoredQuery = useDeleteStoredQuery();

  return async () => {
    if (!datasetId) return;

    try {
      await deleteStoredQuery(datasetId, previousQueryId);

      dispatch(deletePreviousQuerySuccess(previousQueryId));

      if (onSuccess) {
        onSuccess();
      }
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.deleteError") }));
    }
  };
};
