import React from "react";
import styled from "@emotion/styled";
import ReactList from "react-list";

import { FormConfigT } from "./reducer";
import FormConfig from "./FormConfig";

interface PropsT {
  datasetId: string;
  formConfigs: FormConfigT[];
}

const Root = styled("div")`
  flex: 1;
  overflow-y: auto;
  font-size: ${({ theme }) => theme.font.sm};
  padding: 0 10px;
`;
const Container = styled("div")`
  margin: 4px 0;
`;

const FormConfigs: React.FC<PropsT> = ({ datasetId, formConfigs }) => {
  function renderConfig(index: number, key: string | number) {
    return (
      <Container key={key}>
        <FormConfig datasetId={datasetId} config={formConfigs[index]} />
      </Container>
    );
  }

  return (
    <Root>
      <ReactList
        itemRenderer={renderConfig}
        length={formConfigs.length}
        type="variable"
      />
    </Root>
  );
};

export default FormConfigs;
