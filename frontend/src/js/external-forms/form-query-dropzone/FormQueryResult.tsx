import React from "react";
import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";

type PropsType = {
  queryResult?: Object;
  className?: string;
  onDelete?: Function;
};

const Root = styled("div")`
  display: inline-block;
  padding: 5px 10px;
  background-color: white;
  border: 1px solid ${({ theme }) => theme.col.blueGrayLight};
  border-radius: ${({ theme }) => theme.borderRadiusk};
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
`;

const FormQueryResult = (props: PropsType) => {
  return (
    props.queryResult && (
      <Root className={props.className}>
        {props.queryResult.label || props.queryResult.id}
        {!!props.onDelete && (
          <IconButton tiny icon="times" onClick={props.onDelete} />
        )}
      </Root>
    )
  );
};

export default FormQueryResult;
