import { faListUl, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";
import { openHistory, useNewHistorySession } from "./actions";

export const QueryResultHistoryButton = ({
  url,
  label,
  columns,
}: {
  columns: ColumnDescription[];
  label: string;
  url: string;
}) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isLoading,
  );

  const getAuthorizedUrl = useGetAuthorizedUrl();
  const newHistorySession = useNewHistorySession();

  return (
    <Button
      intent="secondary"
      onPress={async () => {
        await newHistorySession(getAuthorizedUrl(url), columns, label);
        dispatch(openHistory());
      }}
    >
      <Icon icon={isLoading ? faSpinner : faListUl} />
      {t("history.history")}
    </Button>
  );
};
