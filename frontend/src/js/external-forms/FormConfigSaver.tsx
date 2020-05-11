import React, { useState, useEffect } from "react";
import { T } from "js/localization";
import styled from "@emotion/styled";
import EditableText from "js/form-components/EditableText";
import { useSelector, useDispatch } from "react-redux";
import { StateT } from "app-types";
import {
  selectActiveFormValues,
  selectActiveFormName,
  selectActiveFormType,
} from "./stateSelectors";
import { postFormConfig, patchFormConfig, getFormConfig } from "js/api/api";
import Label from "js/form-components/Label";
import { setMessage } from "js/snack-message/actions";
import IconButton from "js/button/IconButton";
import { usePrevious } from "js/common/helpers/usePrevious";
import Dropzone from "js/form-components/Dropzone";

import { FORM_CONFIG } from "js/common/constants/dndTypes";
import { FormConfigDragItem } from "./form-configs/FormConfig";
import { loadExternalFormValues, setExternalForm } from "./actions";
import FaIcon from "js/icon/FaIcon";

interface PropsT {
  datasetId: string;
}

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
  margin: 4px 4px 0;
  flex-shrink: 0;
`;

const SxDropzone = styled(Dropzone)`
  color: #333;
`;

const LoadingText = styled("p")`
  font-weight: 400;
  margin: 5px 0 0px 8px;
`;

const SxFaIcon = styled(FaIcon)`
  margin-right: 5px;
`;

const hasChanged = (a: any, b: any) => {
  return JSON.stringify(a) !== JSON.stringify(b);
};

const FormConfigSaver: React.FC<PropsT> = ({ datasetId }) => {
  const dispatch = useDispatch();
  const [editing, setEditing] = useState<boolean>(false);
  const [formConfigId, setFormConfigId] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState<boolean>(true);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  const formValues = useSelector<StateT, Record<string, any>>((state) =>
    selectActiveFormValues(state)
  );
  const previousFormValues = usePrevious(formValues);
  const activeFormName = useSelector<StateT, string>((state) =>
    selectActiveFormName(state)
  );
  const activeFormType = useSelector<StateT, string | null>((state) =>
    selectActiveFormType(state)
  );

  function getUntitledName(name: string) {
    return `${name} ${new Date().toISOString().split("T")[0]}`;
  }

  const [configName, setConfigName] = useState<string>(
    getUntitledName(activeFormName)
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
    setIsSaving(true);
    try {
      if (formConfigId) {
        await patchFormConfig(datasetId, formConfigId, {
          label: configName,
          values: formValues,
        });

        setIsDirty(false);
      } else if (activeFormType) {
        const result = await postFormConfig(datasetId, {
          label: configName,
          formType: activeFormType,
          values: formValues,
        });

        setFormConfigId(result.id);
        setIsDirty(false);
      }
    } catch (e) {
      dispatch(setMessage("externalForms.config.saveError"));
    }
    setIsSaving(false);
  }

  async function onLoad(dragItem: FormConfigDragItem) {
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
      dispatch(setMessage("formConfig.loadError"));
    }
    setIsLoading(false);
  }

  function onDropConfig(props: any, monitor: any) {
    const dragItem = monitor.getItem();

    onLoad(dragItem);
  }

  return (
    <Root>
      <SxDropzone onDrop={onDropConfig} acceptedDropTypes={[FORM_CONFIG]}>
        {() => (
          <SpacedRow>
            <div>
              <Label>{T.translate("externalForms.config.headline")}</Label>
              <Row>
                {isLoading ? (
                  <LoadingText>
                    <SxFaIcon icon="spinner" />
                    {T.translate("common.loading")}
                  </LoadingText>
                ) : (
                  <>
                    <SxEditableText
                      loading={false}
                      editing={editing}
                      onToggleEdit={() => setEditing(!editing)}
                      text={configName || ""}
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
              {T.translate("externalForms.config.save")}
            </IconButton>
          </SpacedRow>
        )}
      </SxDropzone>
    </Root>
  );
};

export default FormConfigSaver;
