import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";
import { exists } from "../../common/helpers/exists";
import type { DragItemQuery } from "../../standard-query-editor/types";

const Root = styled("div")<{ error?: boolean }>`
  padding: 5px 10px;
  background-color: white;
  border-radius: ${({ theme }) => theme.borderRadius};
  border: 1px solid
    ${({ theme, error }) => (error ? theme.col.red : theme.col.grayLight)};
  font-size: ${({ theme }) => theme.font.md};
  color: ${({ theme }) => theme.col.black};
`;

const ErrorMessage = styled.span`
  color: ${({ theme }) => theme.col.red};
  font-weight: 400;
`;

interface PropsT {
  queryResult?: DragItemQuery;
  className?: string;
  error?: string;
  onDelete?: () => void;
}

const FormQueryResult = ({
  queryResult,
  className,
  error,
  onDelete,
}: PropsT) => {
  return (
    <Root className={className} error={exists(error)}>
      {error ? (
        <ErrorMessage>{error}</ErrorMessage>
      ) : queryResult ? (
        queryResult.label || queryResult.id
      ) : null}
      {onDelete && <IconButton tiny icon="times" onClick={onDelete} />}
    </Root>
  );
};

export default FormQueryResult;
