import styled from "@emotion/styled";
import { faListUl, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { openHistory, useNewHistorySession } from "../entity-history/actions";

import { ColumnDescription } from "../api/types";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
  height: 35px;
`;

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
    <SxIconButton
      icon={isLoading ? faSpinner : faListUl}
      frame
      onClick={async () => {
        await newHistorySession(getAuthorizedUrl(url), columns, label);
        dispatch(openHistory());
      }}
    >
      {t("history.history")}
    </SxIconButton>
  );
};
