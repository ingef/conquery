import { useCallback, useEffect, useMemo } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { useDispatch, useSelector, useStore } from "react-redux";

import { useGetForms } from "../api/api";
import type { DatasetT } from "../api/types";
import type { StateT } from "../app/reducers";
import { usePrevious } from "../common/helpers/usePrevious";
import { useActiveLang } from "../localization/useActiveLang";

import FormContainer from "./FormContainer";
import FormsNavigation from "./FormsNavigation";
import FormsQueryRunner from "./FormsQueryRunner";
import { loadFormsSuccess, setExternalForm } from "./actions";
import type { Field, Form, Tabs } from "./config-types";
import type { DynamicFormValues } from "./form/Form";
import { collectAllFormFields, getInitialValue } from "./helper";
import { selectFormConfig } from "./stateSelectors";

const useLoadForms = ({ datasetId }: { datasetId: DatasetT["id"] | null }) => {
  const store = useStore();
  const getForms = useGetForms();
  const dispatch = useDispatch();

  useEffect(() => {
    async function loadForms() {
      if (!datasetId) {
        return;
      }

      const forms = await getForms(datasetId);

      dispatch(loadFormsSuccess({ forms }));

      if (forms.length > 0) {
        dispatch(setExternalForm({ form: forms[0].type }));
      }
    }

    loadForms();
  }, [store, datasetId, getForms, dispatch]);
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

const useInitializeForm = ({
  datasetId,
}: {
  datasetId: DatasetT["id"] | null;
}) => {
  const activeLang = useActiveLang();
  const config = useSelector<StateT, Form | null>(selectFormConfig);
  const allFields: (Field | Tabs)[] = useMemo(() => {
    return config
      ? collectAllFormFields(config.fields).filter(
          (field): field is Field | Tabs => field.type !== "GROUP",
        )
      : [];
  }, [config]);

  const datasetOptions = useDatasetOptions();

  const defaultValues = useMemo(() => {
    if (!config) return {};

    return Object.fromEntries(
      allFields.map((field) => {
        const initialValue = getInitialValue(field, {
          availableDatasets: datasetOptions,
          activeLang,
          datasetId,
        });

        return [field.name, initialValue];
      }),
    );
  }, [allFields, datasetOptions, activeLang, config, datasetId]);

  const methods = useForm<DynamicFormValues>({
    defaultValues,
    mode: "onChange",
  });

  const onReset = useCallback(() => {
    methods.reset(defaultValues);
  }, [methods, defaultValues]);

  const onResetActiveForm = useCallback(() => {
    methods.reset({
      ...methods.getValues(),
      ...defaultValues,
    });
  }, [methods, defaultValues]);

  return { methods, config, datasetOptions, onReset, onResetActiveForm };
};

const FormsTab = () => {
  const datasetId = useSelector<StateT, DatasetT["id"] | null>(
    (state) => state.datasets.selectedDatasetId,
  );
  const previousDatasetId = usePrevious(datasetId);

  useLoadForms({ datasetId });

  const { methods, config, datasetOptions, onReset, onResetActiveForm } =
    useInitializeForm();

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
      <FormsNavigation onReset={onResetActiveForm} />
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
