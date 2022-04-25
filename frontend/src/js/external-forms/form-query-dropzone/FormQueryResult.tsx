import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";

import IconButton from "../../button/IconButton";
import type { DragItemQuery } from "../../standard-query-editor/types";

interface PropsT {
  queryResult?: DragItemQuery;
  className?: string;
  error?: boolean;
  onDelete?: () => void;
}

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

const FormQueryResult = ({
  queryResult,
  className,
  error,
  onDelete,
}: PropsT) => {
  const { t } = useTranslation();

  if (!queryResult) return null;

  return (
    <Root className={className} error={error}>
      {error ? (
        <ErrorMessage>{t("previousQuery.loadError")}</ErrorMessage>
      ) : (
        queryResult.label || queryResult.id
      )}
      {!!onDelete && <IconButton tiny icon="times" onClick={onDelete} />}
    </Root>
  );
};

export default FormQueryResult;
