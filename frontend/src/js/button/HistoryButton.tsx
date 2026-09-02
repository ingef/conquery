import { faListUl } from "@fortawesome/free-solid-svg-icons";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { openHistory } from "../entity-history/actions";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import IconButton from "./IconButton";

export const HistoryButton = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onClick = useCallback(() => {
    dispatch(openHistory());
  }, [dispatch]);

  return (
    <TooltipTrigger>
      <IconButton small frame icon={faListUl} onClick={onClick} />
      <Tooltip>{t("history.history")}</Tooltip>
    </TooltipTrigger>
  );
};
