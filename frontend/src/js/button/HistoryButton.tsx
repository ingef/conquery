import { faListUl } from "@fortawesome/free-solid-svg-icons";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { openHistory } from "../entity-history/actions";
import { Button } from "../ui-components/Button";
import { Icon } from "../ui-components/Icon";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

export const HistoryButton = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onClick = useCallback(() => {
    dispatch(openHistory());
  }, [dispatch]);

  return (
    <TooltipTrigger>
      <Button
        aria-label={t("history.history")}
        intent="secondary"
        onPress={onClick}
      >
        <Icon icon={faListUl} />
      </Button>
      <Tooltip>{t("history.history")}</Tooltip>
    </TooltipTrigger>
  );
};
