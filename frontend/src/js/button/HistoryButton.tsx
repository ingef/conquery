import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openHistory, useNewHistorySession } from "../entity-history/actions";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

interface PropsT {
  columns: ColumnDescription[];
  label: string;
  url: string;
}

const HistoryButton = ({ url, label, columns, ...restProps }: PropsT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isLoading,
  );

  const getAuthorizedUrl = useGetAuthorizedUrl();
  const newHistorySession = useNewHistorySession();

  return (
    <SxIconButton
      icon={isLoading ? "spinner" : "id-badge"}
      frame
      onClick={async () => {
        await newHistorySession(getAuthorizedUrl(url), columns, label);
        dispatch(openHistory());
      }}
      {...restProps}
    >
      {t("history.history")}
    </SxIconButton>
  );
};

export default HistoryButton;
