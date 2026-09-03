import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../ui-components/ConfirmableTooltip";
import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

import { clearQuery } from "./actions";

const QueryClearButton = ({ className }: { className?: string }) => {
  const dispatch = useDispatch();
  const onClearQuery = () => dispatch(clearQuery());
  const { t } = useTranslation();

  return (
    <div className={className}>
      <TooltipTrigger>
        <ConfirmableTooltip
          confirmationText={t(`queryEditor.clearConfirm`)}
          onConfirm={onClearQuery}
        >
          <IconButton tiny icon={faTrash} tabIndex={-1} />
        </ConfirmableTooltip>
        <Tooltip>{t("queryEditor.clear")}</Tooltip>
      </TooltipTrigger>
    </div>
  );
};

export default QueryClearButton;
