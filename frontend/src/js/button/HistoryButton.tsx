import { faListUl } from "@fortawesome/free-solid-svg-icons";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { openHistory } from "../entity-history/actions";
import WithTooltip from "../tooltip/WithTooltip";

import IconButton from "./IconButton";

export const HistoryButton = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onClick = useCallback(() => {
    dispatch(openHistory());
  }, [dispatch]);

  return (
    <WithTooltip text={t("history.history")}>
      <IconButton small frame icon={faListUl} onClick={onClick} />
    </WithTooltip>
  );
};
