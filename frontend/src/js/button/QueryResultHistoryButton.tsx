import { faListUl, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openHistory, useNewHistorySession } from "../entity-history/actions";
import IconButton from "./IconButton";

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
    <IconButton
      className="h-[35px] whitespace-nowrap"
      icon={isLoading ? faSpinner : faListUl}
      frame
      onClick={async () => {
        await newHistorySession(getAuthorizedUrl(url), columns, label);
        dispatch(openHistory());
      }}
    >
      {t("history.history")}
    </IconButton>
  );
};
