import { faTrash } from "@fortawesome/free-solid-svg-icons";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../ui-components/ConfirmableTooltip";
import WithTooltip from "../ui-components/WithTooltip";

import { clearQuery } from "./actions";

const QueryClearButton = ({ className }: { className?: string }) => {
  const dispatch = useDispatch();
  const onClearQuery = () => dispatch(clearQuery());
  const { t } = useTranslation();

  return (
    <div className={className}>
      <ConfirmableTooltip
        confirmationText={t(`queryEditor.clearConfirm`)}
        onConfirm={onClearQuery}
      >
        <WithTooltip text={t("queryEditor.clear")}>
          <IconButton tiny icon={faTrash} tabIndex={-1} />
        </WithTooltip>
      </ConfirmableTooltip>
    </div>
  );
};

export default QueryClearButton;
