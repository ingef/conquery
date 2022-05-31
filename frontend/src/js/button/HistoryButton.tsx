import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { ColumnDescription } from "../api/types";
import type { StateT } from "../app/reducers";
import { useGetAuthorizedUrl } from "../authorization/useAuthorizedUrl";
import { openHistory, useNewHistorySession } from "../entity-history/actions";
import WithTooltip from "../tooltip/WithTooltip";

import IconButton from "./IconButton";

const SxIconButton = styled(IconButton)`
  white-space: nowrap;
  padding: 5px 6px;
`;

interface PropsT {
  columns: ColumnDescription[];
  label: string;
  url: string;
  className?: string;
}

const HistoryButton = ({
  url,
  label,
  columns,
  className,
  ...restProps
}: PropsT) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const isLoading = useSelector<StateT, boolean>(
    (state) => state.entityHistory.isLoading,
  );

  const getAuthorizedUrl = useGetAuthorizedUrl();
  const newHistorySession = useNewHistorySession();

  return (
    <WithTooltip text={t("history.history")} className={className}>
      <SxIconButton
        icon={isLoading ? "spinner" : "id-badge"}
        onClick={async () => {
          await newHistorySession(getAuthorizedUrl(url), columns, label);
          dispatch(openHistory());
        }}
        {...restProps}
      />
    </WithTooltip>
  );
};

export default HistoryButton;
