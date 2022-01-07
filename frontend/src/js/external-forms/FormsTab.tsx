import { tabDescription } from ".";
import { StateT } from "app-types";
import { useCallback, useEffect, useMemo } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useSelector, useStore } from "react-redux";

import { useGetForms } from "../api/api";
import type { DatasetIdT, DatasetT } from "../api/types";
import { usePrevious } from "../common/helpers/usePrevious";
import { useActiveLang } from "../localization/useActiveLang";
import StandardQueryEditorTab from "../standard-query-editor";
import { updateReducers } from "../store";
import TimebasedQueryEditorTab from "../timebased-query-editor";

import FormContainer from "./FormContainer";
import FormsNavigation from "./FormsNavigation";
import FormsQueryRunner from "./FormsQueryRunner";
import type { Form } from "./config-types";
import type { DynamicFormValues } from "./form/Form";
import { collectAllFormFields, getInitialValue } from "./helper";
import buildExternalFormsReducer from "./reducer";
import { selectFormConfig } from "./stateSelectors";

const useLoadForms = ({ datasetId }: { datasetId: DatasetIdT | null }) => {
  const store = useStore();
  const getForms = useGetForms();

  useEffect(() => {
    async function loadForms() {
      if (!datasetId) {
        return;
      }

      const configuredForms = await getForms(datasetId);

      const forms = Object.fromEntries(
        configuredForms.map((form) => [form.type, form]),
      );

      const externalFormsReducer = buildExternalFormsReducer(forms);

      const tabs = [
        StandardQueryEditorTab,
        TimebasedQueryEditorTab,
        {
          ...tabDescription,
          reducer: externalFormsReducer,
        },
      ];

      updateReducers(store, tabs);
    }

    loadForms();
  }, [store, datasetId]);
};

export const useDatasetOptions = () => {
  const availableDatasets = useSelector<StateT, DatasetT[]>(
    (state) => state.datasets.data,
  );

  return useMemo(
    () =>
      availableDatasets.map((dataset) => ({
        label: dataset.label,
        value: dataset.id,
      })),
    [availableDatasets],
  );
};

const useInitializeForm = () => {
  const activeLang = useActiveLang();
  const config = useSelector<StateT, Form | null>(selectFormConfig);
  const allFields = useMemo(() => {
    return config ? collectAllFormFields(config.fields) : [];
  }, [config]);

  const datasetOptions = useDatasetOptions();

  const defaultValues = useMemo(
    () =>
      Object.fromEntries(
        allFields.map((field) => {
          const initialValue = getInitialValue(field, {
            availableDatasets: datasetOptions,
            activeLang,
          });

          return [field.name, initialValue];
        }),
      ),
    [allFields, datasetOptions, activeLang],
  );

  const methods = useForm<DynamicFormValues>({
    defaultValues,
    mode: "onChange",
  });

  const onReset = useCallback(() => {
    methods.reset(defaultValues);
  }, [methods, defaultValues]);

  const previousConfig = usePrevious(config);
  useEffect(
    function resetOnFormChange() {
      if (previousConfig?.type !== config?.type) {
        onReset();
      }
    },
    [previousConfig, config, onReset],
  );

  return { methods, config, datasetOptions, onReset };
};

const FormsTab = () => {
  const datasetId = useSelector<StateT, DatasetIdT | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const previousDatasetId = usePrevious(datasetId);
  useLoadForms({ datasetId });

  const { methods, config, datasetOptions, onReset } = useInitializeForm();

  useEffect(
    function resetOnDatasetChange() {
      if (datasetId && previousDatasetId !== datasetId) {
        onReset();
      }
    },
    [datasetId, previousDatasetId, onReset],
  );

  return (
    <FormProvider {...methods}>
      <FormsNavigation reset={onReset} />
      <FormContainer
        methods={methods}
        config={config}
        datasetOptions={datasetOptions}
      />
      <FormsQueryRunner />
    </FormProvider>
  );
};

export default FormsTab;
