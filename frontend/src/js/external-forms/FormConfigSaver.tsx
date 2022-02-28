import styled from "@emotion/styled";
import type { StateT } from "app-types";
import { FC, useState, useEffect, memo } from "react";
import { useFormContext, useWatch } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import {
  usePatchFormConfig,
  useGetFormConfig,
  usePostFormConfig,
} from "../api/api";
import type { SelectOptionT } from "../api/types";
import IconButton from "../button/IconButton";
import { DNDType } from "../common/constants/dndTypes";
import { usePrevious } from "../common/helpers/usePrevious";
import { useDatasetId } from "../dataset/selectors";
import FaIcon from "../icon/FaIcon";
import { Language, useActiveLang } from "../localization/useActiveLang";
import { setMessage } from "../snack-message/actions";
import WithTooltip from "../tooltip/WithTooltip";
import Dropzone from "../ui-components/Dropzone";
import EditableText from "../ui-components/EditableText";
import Label from "../ui-components/Label";

import { setExternalForm } from "./actions";
import type { Form, FormField } from "./config-types";
import type { FormConceptGroupT } from "./form-concept-group/formConceptGroupState";
import type { DragItemFormConfig } from "./form-configs/FormConfig";
import type { FormConfigT } from "./form-configs/reducer";
import { useLoadFormConfigs } from "./form-configs/selectors";
import { collectAllFormFields } from "./helper";
import {
  useSelectActiveFormName,
  selectActiveFormType,
  selectFormConfig,
} from "./stateSelectors";

const Root = styled("div")`
  display: flex;
  align-items: center;
  margin: 5px 0 20px;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SxLabel = styled(Label)`
  margin: 0;
`;

const SxEditableText = styled(EditableText)<{ editing: boolean }>`
  margin: ${({ editing }) => (editing ? "" : "5px 0 0px 8px")};
  font-weight: 700;
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

const SpacedRow = styled(Row)`
  justify-content: space-between;
  width: 100%;
`;

const DirtyFlag = styled("div")`
  width: 7px;
  height: 7px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  margin: 0 4px;
  flex-shrink: 0;
`;

const SxDropzone = styled(Dropzone)`
  color: #333;
`;

const LoadingText = styled("p")`
  font-weight: 400;
  margin: 3px 0 0px 8px;
`;

const SxFaIcon = styled(FaIcon)`
  margin-right: 5px;
`;

const SxWithTooltip = styled(WithTooltip)`
  flex-shrink: 0;
`;

const DROP_TYPES = [DNDType.FORM_CONFIG];

const hasChanged = (a: any, b: any) => {
  return JSON.stringify(a) !== JSON.stringify(b);
};

// Potentially transform the stored field value to support older saved form configs
//
// because we changed the SELECT values:
// from string, e.g. 'next'
// to SelectValueT, e.g. { value: 'next', label: 'Next' }
//
// and because we introduced the DNDTypes (CONCEPT_LIST)
const transformLoadedFieldValue = (
  field: FormField,
  value: unknown,
  {
    activeLang,
    datasetOptions,
  }: { activeLang: Language; datasetOptions: SelectOptionT[] },
) => {
  switch (field.type) {
    case "CONCEPT_LIST":
      return (value as FormConceptGroupT[]).map((group) => ({
        ...group,
        concepts: group.concepts.map((concept) => ({
          ...concept,
          type: DNDType.CONCEPT_TREE_NODE,
        })),
      }));
    case "DATASET_SELECT":
      if (typeof value === "object") return value as SelectOptionT;
      if (typeof value === "string") {
        return datasetOptions.find((option) => option.value === value);
      }

      return value;
    case "SELECT":
      if (typeof value === "object") return value as SelectOptionT;
      if (typeof value === "string") {
        const options = field.options.map((option) => ({
          label: option.label[activeLang] || "",
          value: option.value,
        }));

        return options.find((option) => option.value === value);
      }
      return value;
    default:
      return value;
  }
};

interface Props {
  datasetOptions: SelectOptionT[];
}

const FormConfigSaver: FC<Props> = ({ datasetOptions }) => {
  const { t } = useTranslation();
  const activeLang = useActiveLang();
  const dispatch = useDispatch();
  const datasetId = useDatasetId();
  const [editing, setEditing] = useState<boolean>(false);
  const [formConfigId, setFormConfigId] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [formConfigToLoadNext, setFormConfigToLoadNext] =
    useState<FormConfigT | null>(null);

  const activeFormName = useSelectActiveFormName();
  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );

  const { setValue } = useFormContext();
  const formValues = useWatch({});
  const previousFormValues = usePrevious(formValues);

  const { loadFormConfigs } = useLoadFormConfigs();
  const formConfig = useSelector<StateT, Form | null>(selectFormConfig);

  const postFormConfig = usePostFormConfig();
  const getFormConfig = useGetFormConfig();
  const patchFormConfig = usePatchFormConfig();

  function getUntitledName(name: string) {
    return `${name} ${new Date().toISOString().split("T")[0]}`;
  }

  const [configName, setConfigName] = useState<string>(
    getUntitledName(activeFormName),
  );

  useEffect(() => {
    setConfigName(getUntitledName(activeFormName));
  }, [activeFormName]);

  useEffect(() => {
    setIsDirty(true);
    setFormConfigId(null);
  }, [configName]);

  useEffect(() => {
    if (hasChanged(previousFormValues, formValues)) {
      setIsDirty(true);
    }
  }, [formValues, previousFormValues]);

  useEffect(
    function deferredLoadFormConfig() {
      // Needs to be deferred because the form type might get changed
      // and other effects will have to run to reset / initialize the form first
      // before we can load new values into it
      if (!formConfig) return;

      if (formConfigToLoadNext) {
        setFormConfigToLoadNext(null);

        const entries = Object.entries(formConfigToLoadNext.values);

        for (const [fieldname, value] of entries) {
          // --------------------------
          // Potentially transform the stored field value to support older saved form configs
          // because we changed the SELECT values:
          // from string, e.g. 'next'
          // to SelectValueT, e.g. { value: 'next', label: 'Next' }
          const field = collectAllFormFields(formConfig.fields).find(
            (f) => f.type !== "GROUP" && f.name === fieldname,
          );

          if (!field) continue;

          const fieldValue = transformLoadedFieldValue(field, value, {
            activeLang,
            datasetOptions,
          });
          // --------------------------

          setValue(fieldname, fieldValue, {
            shouldValidate: true,
            shouldDirty: true,
            shouldTouch: true,
          });
        }

        setConfigName(formConfigToLoadNext.label);
        setIsDirty(false);
      }
    },
    [formConfigToLoadNext, formConfig, activeLang, datasetOptions, setValue],
  );

  async function onSubmit() {
    if (!datasetId) return;

    setIsSaving(true);
    try {
      if (formConfigId) {
        await patchFormConfig(datasetId, formConfigId, {
          label: configName,
          values: formValues,
        });

        setIsDirty(false);
        loadFormConfigs(datasetId);
      } else if (activeFormType) {
        const result = await postFormConfig(datasetId, {
          label: configName,
          formType: activeFormType,
          values: formValues,
        });

        setFormConfigId(result.id);
        setIsDirty(false);
        loadFormConfigs(datasetId);
      }
    } catch (e) {
      dispatch(setMessage({ message: t("externalForms.config.saveError") }));
    }
    setIsSaving(false);
  }

  async function onLoad(dragItem: DragItemFormConfig) {
    if (!datasetId) return;

    setIsLoading(true);
    setIsDirty(false);
    try {
      const config = await getFormConfig(datasetId, dragItem.id);
      setIsLoading(false);

      if (config.formType !== activeFormType) {
        dispatch(setExternalForm({ form: config.formType }));
      }

      setFormConfigToLoadNext(config);
    } catch (e) {
      dispatch(setMessage({ message: t("formConfig.loadError") }));
      setIsLoading(false);
    }
  }

  return (
    <Root>
      <SxDropzone /* TODO: ADD GENERIC TYPE <FC<DropzoneProps<DragItemFormConfig>>> */
        onDrop={(item) => onLoad(item as DragItemFormConfig)}
        acceptedDropTypes={DROP_TYPES}
      >
        {() => (
          <SpacedRow>
            <div>
              <SxLabel>{t("externalForms.config.headline")}</SxLabel>
              <Row>
                {isLoading ? (
                  <LoadingText>
                    <SxFaIcon icon="spinner" />
                    {t("common.loading")}
                  </LoadingText>
                ) : (
                  <>
                    <SxEditableText
                      loading={false}
                      editing={editing}
                      onToggleEdit={() => setEditing(!editing)}
                      text={configName || ""}
                      saveOnClickoutside
                      onSubmit={(txt: string) => {
                        if (txt) {
                          setConfigName(txt);
                        }
                        setEditing(false);
                      }}
                    />
                    {isDirty && <DirtyFlag />}
                  </>
                )}
              </Row>
            </div>
            <SxWithTooltip
              lazy
              text={t("externalForms.config.saveDescription")}
            >
              <IconButton
                frame
                icon={isSaving ? "spinner" : "save"}
                onClick={onSubmit}
              >
                {t("externalForms.config.save")}
              </IconButton>
            </SxWithTooltip>
          </SpacedRow>
        )}
      </SxDropzone>
    </Root>
  );
};

export default memo(FormConfigSaver);
