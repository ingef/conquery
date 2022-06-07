import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openHistory, useInitHistorySession } from "../entity-history/actions";
import WithTooltip from "../tooltip/WithTooltip";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
`;

interface PropsT {
  columns: ColumnDescription[];
  url: string;
  className?: string;
}

const HistoryButton = ({ url, columns, className, ...restProps }: PropsT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.preview.isLoading,
  );

  const getAuthorizedUrl = useGetAuthorizedUrl();
  const initHistorySession = useInitHistorySession();

  return (
    <WithTooltip text={t("history.history")} className={className}>
      <SxIconButton
        icon={isLoading ? "spinner" : "book"}
        onClick={async () => {
          await initHistorySession(getAuthorizedUrl(url), columns);
          dispatch(openHistory());
        }}
        {...restProps}
      />
    </WithTooltip>
  );
};

export default HistoryButton;
