import { StateT } from "app-types";
import { useCallback, useState } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import { useGetFormConfig, useGetFormConfigs } from "../../api/api";
import type { DatasetIdT } from "../../api/types";
import { setMessage } from "../../snack-message/actions";
import { useActiveFormType } from "../stateSelectors";

import {
  loadFormConfigsError,
  loadFormConfigsSuccess,
  patchFormConfigSuccess,
} from "./actions";
import { FormConfigT } from "./reducer";

const configHasTag = (config: FormConfigT, searchTerm: string) => {
  return (
    !!config.tags &&
    config.tags.some((tag) => {
      return tag.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1;
    })
  );
};

const configHasLabel = (config: FormConfigT, searchTerm: string) => {
  return (
    config.label &&
    config.label.toLowerCase().indexOf(searchTerm.toLowerCase()) !== -1
  );
};

const configHasId = (config: FormConfigT, searchTerm: string) => {
  return config.id.toString() === searchTerm;
};

const configHasFilterType = (
  config: FormConfigT,
  filter: string,
  { activeFormType }: { activeFormType: string | null },
) => {
  if (filter === "all") return true;

  if (filter === "activeForm")
    return !!activeFormType && config.formType === activeFormType;

  // Checks config.own, config.shared or config.system
  if (config[filter]) return true;

  // Special case for a "system"-config:
  // it's simply not shared and not self-created (own)
  if (filter === "system" && !config.shared && !config.own) return true;

  return false;
};

export const useFilteredFormConfigs = () => {
  const formConfigs = useSelector<StateT, FormConfigT[]>(
    (state) => state.formConfigs.data,
  );
  const search = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch,
  );
  const filter = useSelector<StateT, string>(
    (state) => state.formConfigsFilter,
  );

  const activeFormType = useActiveFormType();

  if (search.length === 0 && filter === "all") return formConfigs;

  return formConfigs.filter((config) => {
    return (
      configHasFilterType(config, filter, { activeFormType }) &&
      search.every((searchTerm) => {
        return (
          configHasId(config, searchTerm) ||
          configHasLabel(config, searchTerm) ||
          configHasTag(config, searchTerm)
        );
      })
    );
  });
};

const labelContainsAnySearch = (label: string, searches: string[]) =>
  searches.some(
    (search) => label.toLowerCase().indexOf(search.toLowerCase()) !== -1,
  );

export const useIsLabelHighlighted = (label: string) => {
  const formConfigsSearch = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch,
  );

  return labelContainsAnySearch(label, formConfigsSearch);
};

export const useLoadFormConfigs = () => {
  const [loading, setLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const getFormConfigs = useGetFormConfigs();

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
    [dispatch],
  );

  return {
    loading,
    loadFormConfigs,
  };
};

export const useLoadFormConfig = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState<boolean>(false);
  const dispatch = useDispatch();
  const getFormConfig = useGetFormConfig();

  const loadFormConfig = useCallback(
    async (datasetId: DatasetIdT, id: string) => {
      setLoading(true);
      try {
        const data = await getFormConfig(datasetId, id);

        dispatch(patchFormConfigSuccess(id, data));
      } catch (e) {
        dispatch(setMessage({ message: t("formConfig.loadError") }));
      }
      setLoading(false);
    },
    [t, dispatch],
  );

  return {
    loading,
    loadFormConfig,
  };
};
