import { TrashIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";
import { Button } from "../ui-components/Button";
import { ConfirmMenu } from "../ui-components/ConfirmMenu";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import { clearQuery } from "./actions";

const QueryClearButton = ({ className }: { className?: string }) => {
  const dispatch = useDispatch();
  const onClearQuery = () => dispatch(clearQuery());
  const { t } = useTranslation();

  return (
    <div className={className}>
      <TooltipTrigger>
        <ConfirmMenu
          confirmationText={t(`queryEditor.clearConfirm`)}
          onConfirm={onClearQuery}
        >
          <Button
            aria-label={t("queryEditor.clear")}
            data-test-id="clear-query"
            intent="tertiary"
            size="sm"
            excludeFromTabOrder
          >
            <TrashIcon />
          </Button>
        </ConfirmMenu>
        <Tooltip>{t("queryEditor.clear")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default QueryClearButton;
