import React, { FC } from "react";
import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";
import type { PreviousQueryT } from "../../previous-queries/list/reducer";

interface PropsT {
  queryResult?: PreviousQueryT;
  className?: string;
  onDelete?: () => void;
}

const Root = styled("div")`
  display: inline-block;
  padding: 5px 10px;
  background-color: white;
  border: 1px solid ${({ theme }) => theme.col.blueGrayLight};
  border-radius: ${({ theme }) => theme.borderRadius};
  box-shadow: 0 0 3px 0 rgba(0, 0, 0, 0.1);
  font-size: 14px;
`;

const FormQueryResult: FC<PropsT> = ({ queryResult, className, onDelete }) => {
  if (!queryResult) return null;

  return (
    <Root className={className}>
      {queryResult.label || queryResult.id}
      {!!onDelete && <IconButton tiny icon="times" onClick={onDelete} />}
    </Root>
  );
};

export default FormQueryResult;
