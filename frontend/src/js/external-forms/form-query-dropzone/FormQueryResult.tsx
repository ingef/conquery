import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";
import type { DragItemQuery } from "../../standard-query-editor/types";

interface PropsT {
  queryResult?: DragItemQuery;
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

const FormQueryResult = ({ queryResult, className, onDelete }: PropsT) => {
  if (!queryResult) return null;

  return (
    <Root className={className}>
      {queryResult.label || queryResult.id}
      {!!onDelete && <IconButton tiny icon="times" onClick={onDelete} />}
    </Root>
  );
};

export default FormQueryResult;
