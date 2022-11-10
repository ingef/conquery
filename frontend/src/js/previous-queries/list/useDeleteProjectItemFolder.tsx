import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import type { StateT } from "../../app/reducers";
import { useDatasetId } from "../../dataset/selectors";
import { setMessage } from "../../snack-message/actions";

import {
  removeFolder,
  useLoadFormConfigs,
  useLoadQueries,
  useUpdateFormConfig,
  useUpdateQuery,
} from "./actions";
import type { FormConfigT, PreviousQueryT } from "./reducer";

export const useDeleteProjectItemFolder = () => {
  const { t } = useTranslation();
  const datasetId = useDatasetId();
  const dispatch = useDispatch();
  const { updateQuery } = useUpdateQuery();
  const { updateFormConfig } = useUpdateFormConfig();
  const { loadQueries } = useLoadQueries();
  const { loadFormConfigs } = useLoadFormConfigs();

  const queries = useSelector<StateT, PreviousQueryT[]>(
    (state) => state.previousQueries.queries,
  );
  const formConfigs = useSelector<StateT, FormConfigT[]>(
    (state) => state.previousQueries.formConfigs,
  );

  const localFolders = useSelector<StateT, string[]>(
    (state) => state.previousQueries.localFolders,
  );

  return async (folder: string) => {
    if (!datasetId) return;

    if (localFolders.includes(folder)) {
      dispatch(removeFolder({ folderName: folder }));
      return;
    }

    try {
      await Promise.all([
        ...queries
          .filter((query) => query.tags.includes(folder))
          .map((query) => {
            const nextTags = query.tags.filter((tag) => tag !== folder);

            return updateQuery(
              query.id,
              { tags: nextTags },
              t("previousQuery.retagError"),
            );
          }),
        ...formConfigs
          .filter((config) => config.tags.includes(folder))
          .map((config) => {
            const nextTags = config.tags.filter((tag) => tag !== folder);

            return updateFormConfig(
              config.id,
              { tags: nextTags },
              t("formConfig.retagError"),
            );
          }),
      ]);

      return Promise.all([loadQueries(datasetId), loadFormConfigs(datasetId)]);
    } catch (e) {
      dispatch(setMessage({ message: t("previousQuery.retagError") }));
      return Promise.reject();
    }
  };
};
