import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription, ResultUrlsWithLabel } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openHistory, useNewHistorySession } from "../entity-history/actions";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
  height: 35px;
`;

interface PropsT {
  columns: ColumnDescription[];
  label: string;
  url: ResultUrlsWithLabel;
}

export const QueryResultHistoryButton = ({ url, label, columns }: PropsT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isLoading,
  );

  const getAuthorizedUrl = useGetAuthorizedUrl();
  const newHistorySession = useNewHistorySession();

  return (
    <SxIconButton
      icon={isLoading ? "spinner" : "list-ul"}
      frame
      onClick={async () => {
        await newHistorySession(
          { ...url, url: getAuthorizedUrl(url.url) },
          columns,
          label,
        );
        dispatch(openHistory());
      }}
    >
      {t("history.history")}
    </SxIconButton>
  );
};
