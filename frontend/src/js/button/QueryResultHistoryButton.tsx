import styled from "@emotion/styled";
import { faListUl, faSpinner } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { openHistory, useNewHistorySession } from "../entity-history/actions";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
  height: 35px;
`;

interface PropsT {
  label: string;
}

export const QueryResultHistoryButton = ({ label }: PropsT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isLoading,
  );

  const newHistorySession = useNewHistorySession();

  return (
    <SxIconButton
      icon={isLoading ? faSpinner : faListUl}
      frame
      onClick={async () => {
        await newHistorySession(label);
        dispatch(openHistory());
      }}
    >
      {t("history.history")}
    </SxIconButton>
  );
};
