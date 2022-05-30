import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { ActionType, createAction } from "typesafe-actions";

import {
  useGetQueries,
  usePatchQuery,
  useGetQuery,
  useDeleteQuery,
  usePatchFormConfig,
  useDeleteFormConfig,
  useGetFormConfig,
  useGetFormConfigs,
} from "../../api/api";
import {
  DatasetT,
  GetQueriesResponseT,
  GetQueryResponseT,
  QueryIdT,
} from "../../api/types";
import { useDatasetId } from "../../dataset/selectors";
import { setMessage } from "../../snack-message/actions";

import type { FormConfigT, PreviousQueryIdT, PreviousQueryT } from "./reducer";

export type PreviousQueryListActions = ActionType<
  | typeof loadQueriesSuccess
  | typeof loadQuerySuccess
  | typeof patchQuerySuccess
  | typeof deleteQuerySuccess
  | typeof addFolder
  | typeof removeFolder
  | typeof loadFormConfigsSuccess
  | typeof patchFormConfigSuccess
  | typeof deleteFormConfigSuccess
>;

export const loadQueriesSuccess = createAction("queries/LOAD_QUERIES_SUCCESS")<{
  data: GetQueriesResponseT;
}>();

export const useLoadQueries = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const getQueries = useGetQueries();

  const [loading, setLoading] = useState(false);

  const loadQueries = async (datasetId: DatasetT["id"]) => {
    setLoading(true);
    try {
      const data = await getQueries(datasetId);

      dispatch(loadQueriesSuccess({ data }));
    } catch (e) {
      dispatch(setMessage({ message: t("previousQueries.error") }));
    }
    setLoading(false);
  };

  return {
    loading,
    loadQueries,
  };
};

export const loadQuerySuccess = createAction("queries/LOAD_QUERY_SUCCESS")<{
  id: QueryIdT;
  data: GetQueryResponseT;
}>();

export const useLoadQuery = () => {
  const { t } = useTranslation();
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const getQuery = useGetQuery();

  const [loading, setLoading] = useState(false);

  const loadQuery = async (queryId: PreviousQueryIdT) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      const query = await getQuery(datasetId, queryId);

      dispatch(loadQuerySuccess({ id: queryId, data: query }));
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.loadError") }));
    }
    setLoading(false);
  };

  return {
    loading,
    loadQuery,
  };
};

export const patchQuerySuccess = createAction("query/UPDATE_SUCCESS")<{
  id: PreviousQueryT["id"];
  data: Partial<PreviousQueryT>;
}>();

export const useUpdateQuery = () => {
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const patchQuery = usePatchQuery();

  const [loading, setLoading] = useState(false);

  const updateQuery = async (
    id: QueryIdT,
    attributes: {
      shared?: boolean;
      groups?: string[];
      label?: string;
      tags?: string[];
    },
    errorMessage: string,
  ) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      await patchQuery(datasetId, id, attributes);

      dispatch(patchQuerySuccess({ id, data: attributes }));
    } catch (e) {
      dispatch(setMessage({ message: errorMessage }));
    }
    setLoading(false);
  };

  return { loading, updateQuery };
};

export const deleteQuerySuccess = createAction("queries/DELETE_QUERY_SUCCESS")<{
  queryId: string;
}>();

export const useRemoveQuery = () => {
  const { t } = useTranslation();
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const deleteQuery = useDeleteQuery();
  const [loading, setLoading] = useState(false);

  const removeQuery = async (queryId: PreviousQueryIdT) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      await deleteQuery(datasetId, queryId);

      dispatch(deleteQuerySuccess({ queryId }));
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.deleteError") }));
    }
    setLoading(false);
  };

  return { loading, removeQuery };
};

// ---------------------
// Local folders
// ---------------------

export const addFolder = createAction("queries/ADD_FOLDER")<{
  folderName: string;
}>();
export const removeFolder = createAction("queries/REMOVE_FOLDER")<{
  folderName: string;
}>();

// ---------------------
// FORM CONFIGS
// ---------------------

export const loadFormConfigsSuccess = createAction(
  "form-configs/LOAD_SUCCESS",
)<{ data: FormConfigT[] }>();

export const useLoadFormConfigs = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const getFormConfigs = useGetFormConfigs();

  const loadFormConfigs = useCallback(
    async (datasetId: DatasetT["id"]) => {
      setLoading(true);
      try {
        const data = await getFormConfigs(datasetId);

        dispatch(loadFormConfigsSuccess({ data }));
      } catch (e) {
        dispatch(setMessage({ message: t("formConfigs.error") }));
      }
      setLoading(false);
    },
    [dispatch, getFormConfigs, t],
  );

  return {
    loading,
    loadFormConfigs,
  };
};

export const patchFormConfigSuccess = createAction(
  "form-config/UPDATE_SUCCESS",
)<{ id: FormConfigT["id"]; data: Partial<FormConfigT> }>();

export const useLoadFormConfig = () => {
  const { t } = useTranslation();
  const datasetId = useDatasetId();
  const [loading, setLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const getFormConfig = useGetFormConfig();

  const loadFormConfig = useCallback(
    async (id: string) => {
      if (!datasetId) return;

      setLoading(true);
      try {
        const data = await getFormConfig(datasetId, id);

        dispatch(patchFormConfigSuccess({ id, data }));
      } catch (e) {
        dispatch(setMessage({ message: t("formConfig.loadError") }));
      }
      setLoading(false);
    },
    [t, getFormConfig, dispatch, datasetId],
  );

  return {
    loading,
    loadFormConfig,
  };
};

export const useUpdateFormConfig = () => {
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const patchFormConfig = usePatchFormConfig();

  const [loading, setLoading] = useState(false);

  const updateFormConfig = async (
    configId: string,
    attributes: {
      shared?: boolean;
      groups?: string[];
      label?: string;
      tags?: string[];
    },
    errorMessage: string,
  ) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      await patchFormConfig(datasetId, configId, attributes);

      dispatch(patchFormConfigSuccess({ id: configId, data: attributes }));
    } catch (e) {
      dispatch(setMessage({ message: errorMessage }));
    }
    setLoading(false);
  };

  return { loading, updateFormConfig };
};

export const deleteFormConfigSuccess = createAction(
  "form-config/DELETE_SUCCESS",
)<{ configId: string }>();

export const useRemoveFormConfig = () => {
  const { t } = useTranslation();
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const deleteFormConfig = useDeleteFormConfig();

  const [loading, setLoading] = useState(false);

  const removeFormConfig = async (configId: string) => {
    if (!datasetId) return;

    setLoading(true);
    try {
      await deleteFormConfig(datasetId, configId);

      dispatch(deleteFormConfigSuccess({ configId }));
    } catch (e) {
      dispatch(setMessage({ message: t("formConfig.deleteError") }));
    }
    setLoading(false);
  };

  return { loading, removeFormConfig };
};
