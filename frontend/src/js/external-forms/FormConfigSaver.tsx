import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC, useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import {
  usePatchFormConfig,
  useGetFormConfig,
  usePostFormConfig,
} from "../api/api";
import IconButton from "../button/IconButton";
import { FORM_CONFIG } from "../common/constants/dndTypes";
import { usePrevious } from "../common/helpers/usePrevious";
import { useDatasetId } from "../dataset/selectors";
import Dropzone from "../form-components/Dropzone";
import EditableText from "../form-components/EditableText";
import Label from "../form-components/Label";
import FaIcon from "../icon/FaIcon";
import { setMessage } from "../snack-message/actions";

import { loadExternalFormValues, setExternalForm } from "./actions";
import { DragItemFormConfig } from "./form-configs/FormConfig";
import { useLoadFormConfigs } from "./form-configs/selectors";
import {
  selectActiveFormValues,
  useSelectActiveFormName,
  selectActiveFormType,
} from "./stateSelectors";

const Root = styled("div")`
  display: flex;
  align-items: center;
  margin: 5px 0 20px;
  border-radius: ${({ theme }) => theme.borderRadius};
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

const hasChanged = (a: any, b: any) => {
  return JSON.stringify(a) !== JSON.stringify(b);
};

const FormConfigSaver: FC = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const datasetId = useDatasetId();
  const [editing, setEditing] = useState<boolean>(false);
  const [formConfigId, setFormConfigId] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const formValues = useSelector<StateT, Record<string, any>>((state) =>
    selectActiveFormValues(state),
  );
  const previousFormValues = usePrevious(formValues);
  const activeFormName = useSelectActiveFormName();
  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state),
  );

  const { loadFormConfigs } = useLoadFormConfigs();

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

      if (config.formType !== activeFormType) {
        dispatch(setExternalForm(config.formType));
      }

      dispatch(loadExternalFormValues(config.formType, config.values));
      setConfigName(config.label);
      setIsDirty(false);
    } catch (e) {
      dispatch(setMessage({ message: t("formConfig.loadError") }));
    }
    setIsLoading(false);
  }

  return (
    <Root>
      <SxDropzone<FC<DropzoneProps<DragItemFormConfig>>>
        onDrop={onLoad}
        acceptedDropTypes={[FORM_CONFIG]}
      >
        {() => (
          <SpacedRow>
            <div>
              <Label>{t("externalForms.config.headline")}</Label>
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
            <IconButton
              frame
              icon={isSaving ? "spinner" : "save"}
              onClick={onSubmit}
            >
              {t("externalForms.config.save")}
            </IconButton>
          </SpacedRow>
        )}
      </SxDropzone>
    </Root>
  );
};

export default FormConfigSaver;
