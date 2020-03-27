import React, { useState, useEffect } from "react";
import { T } from "js/localization";
import styled from "@emotion/styled";
import EditableText from "js/form-components/EditableText";
import { useSelector, useDispatch } from "react-redux";
import { StateT } from "app-types";
import {
  selectActiveFormValues,
  selectActiveFormName,
  selectActiveForm
} from "./stateSelectors";
import { postFormConfig, patchFormConfig } from "js/api/api";
import Label from "js/form-components/Label";
import { setMessage } from "js/snack-message/actions";
import IconButton from "js/button/IconButton";
import { usePrevious } from "js/common/helpers/usePrevious";

interface PropsT {
  datasetId: string;
}

const Root = styled("div")`
  display: flex;
  padding: 5px 20px 15px 10px;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #ccc;
  margin: 0 0 20px;
`;

const SxEditableText = styled(EditableText)<{ editing: boolean }>`
  margin: ${({ editing }) => (editing ? "" : "5px 0 0px 8px")};
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
`;

const DirtyFlag = styled("div")`
  width: 7px;
  height: 7px;
  background-color: ${({ theme }) => theme.col.blueGrayDark};
  border-radius: 50%;
  margin: 4px 4px 0;
`;

const hasChanged = (a: any, b: any) => {
  return JSON.stringify(a) !== JSON.stringify(b);
};

const FormConfigSaver: React.FC<PropsT> = ({ datasetId }) => {
  const dispatch = useDispatch();
  const [editing, setEditing] = useState<boolean>(false);
  const [formConfigId, setFormConfigId] = useState<string | null>(null);
  const [isDirty, setIsDirty] = useState<boolean>(true);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const hasActiveForm = useSelector<StateT, boolean>(
    state => !!selectActiveForm(state)
  );
  const formValues = useSelector<StateT>(state =>
    selectActiveFormValues(state)
  );
  const previousFormValues = usePrevious(formValues);
  const activeFormName = useSelector<StateT, string>(state =>
    selectActiveFormName(state)
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
    setIsLoading(true);
    try {
      if (formConfigId) {
        await patchFormConfig(datasetId, formConfigId, configName, formValues);

        setIsDirty(false);
      } else {
        const result = await postFormConfig(datasetId, configName, formValues);

        setFormConfigId(result.id);
        setIsDirty(false);
      }
    } catch (e) {
      dispatch(setMessage("externalForms.config.saveError"));
    }
    setIsLoading(false);
  }

  if (!hasActiveForm) return null;

  return (
    <Root>
      <div>
        <Label>{T.translate("externalForms.config.headline")}</Label>
        <Row>
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
        </Row>
      </div>
      <IconButton
        frame
        icon={isLoading ? "spinner" : "save"}
        onClick={onSubmit}
      >
        {T.translate("externalForms.config.save")}
      </IconButton>
    </Root>
  );
};

export default FormConfigSaver;
