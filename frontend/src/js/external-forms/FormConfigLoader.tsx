import styled from "@emotion/styled";
import type { StateT } from "app-types";
import { FC, useState, useEffect, memo } from "react";
import { useFormContext } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import { useGetFormConfig } from "../api/api";
import type { SelectOptionT } from "../api/types";
import { DNDType } from "../common/constants/dndTypes";
import { useDatasetId } from "../dataset/selectors";
import FaIcon from "../icon/FaIcon";
import { Language, useActiveLang } from "../localization/useActiveLang";
import type { FormConfigT } from "../previous-queries/list/reducer";
import { setMessage } from "../snack-message/actions";
import Dropzone from "../ui-components/Dropzone";
import Label from "../ui-components/Label";

import { setExternalForm } from "./actions";
import type { Form, FormField } from "./config-types";
import type { FormConceptGroupT } from "./form-concept-group/formConceptGroupState";
import { collectAllFormFields } from "./helper";
import { selectActiveFormType, selectFormConfig } from "./stateSelectors";
import type { DragItemFormConfig } from "./types";

const Root = styled("div")`
  display: flex;
  align-items: center;
  margin: 5px 0 20px;
  border-radius: ${({ theme }) => theme.borderRadius};
`;

const SxLabel = styled(Label)`
  margin: 0;
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

const SpacedRow = styled(Row)`
  justify-content: space-between;
  width: 100%;
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

const DROP_TYPES = [DNDType.FORM_CONFIG];

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
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [formConfigToLoadNext, setFormConfigToLoadNext] =
    useState<FormConfigT | null>(null);

  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );

  const { setValue } = useFormContext();

  const formConfig = useSelector<StateT, Form | null>(selectFormConfig);

  const getFormConfig = useGetFormConfig();

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
      }
    },
    [formConfigToLoadNext, formConfig, activeLang, datasetOptions, setValue],
  );

  async function onLoad(dragItem: DragItemFormConfig) {
    if (!datasetId) return;

    setIsLoading(true);
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
              {formConfigToLoadNext ? (
                <p>{formConfigToLoadNext.label}</p>
              ) : (
                <>
                  <SxLabel>{t("externalForms.loader.headline")}</SxLabel>
                  {isLoading && (
                    <LoadingText>
                      <SxFaIcon icon="spinner" />
                      {t("common.loading")}
                    </LoadingText>
                  )}
                </>
              )}
            </div>
          </SpacedRow>
        )}
      </SxDropzone>
    </Root>
  );
};

export default memo(FormConfigSaver);
