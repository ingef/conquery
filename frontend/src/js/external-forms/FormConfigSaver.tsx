import React, { useState } from "react";
import BasicButton from "js/button/BasicButton";
import { T } from "js/localization";
import styled from "@emotion/styled";
import EditableText from "js/form-components/EditableText";

interface PropsT {
  datasetId: string;
}

const Root = styled("div")`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

const FormConfigSaver: React.FC<PropsT> = () => {
  const [editing, setEditing] = useState<boolean>(false);
  const [configName, setConfigName] = useState<string | null>(null);

  return (
    <Root>
      <EditableText
        loading={false}
        editing={editing}
        onToggleEdit={() => setEditing(!editing)}
        text={configName || ""}
        onSubmit={(txt: string) => setConfigName(txt)}
      />
      <BasicButton>{T.translate("externalForms.config.save")}</BasicButton>
    </Root>
  );
};

export default FormConfigSaver;
