import styled from "@emotion/styled";
import { FC } from "react";

import IconButton from "../../button/IconButton";
import type { PreviousQueryT } from "../../previous-queries/list/reducer";

interface PropsT {
  queryResult?: PreviousQueryT;
  className?: string;
  onDelete?: () => void;
}

const Root = styled("div")`
  padding: 5px 10px;
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid ${({ theme }) => theme.col.grayLight};
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.black};
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
