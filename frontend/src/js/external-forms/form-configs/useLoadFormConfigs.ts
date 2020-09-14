import { useState, useCallback } from "react";
import { getFormConfigs } from "../../api/api";
import { loadFormConfigsSuccess, loadFormConfigsError } from "./actions";
import type { DatasetIdT } from "../../api/types";
import { useDispatch } from "react-redux";

export const useLoadFormConfigs = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const dispatch = useDispatch();

  const loadFormConfigs = useCallback(
    async (datasetId: DatasetIdT) => {
      setLoading(true);
      try {
        const data = await getFormConfigs(datasetId);

        dispatch(loadFormConfigsSuccess(data));
      } catch (e) {
        dispatch(loadFormConfigsError(e));
      }
      setLoading(false);
    },
    [dispatch]
  );

  return {
    loading,
    loadFormConfigs,
  };
};
