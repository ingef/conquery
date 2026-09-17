import { ListIcon } from "lucide-react";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { Button } from "../ui-components/Button";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";
import { openHistory } from "./actions";

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
        <ListIcon />
      </Button>
      <Tooltip>{t("history.history")}</Tooltip>
    </TooltipTrigger>
  );
};
