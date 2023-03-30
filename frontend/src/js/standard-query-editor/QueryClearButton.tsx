import { faTrashAlt } from "@fortawesome/free-regular-svg-icons";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import IconButton from "../button/IconButton";
import { ConfirmableTooltip } from "../tooltip/ConfirmableTooltip";
import WithTooltip from "../tooltip/WithTooltip";

import { clearQuery } from "./actions";

interface PropsT {
  className?: string;
}

const QueryClearButton: FC<PropsT> = ({ className }) => {
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
          <IconButton tiny icon={faTrashAlt} tabIndex={-1} />
        </WithTooltip>
      </ConfirmableTooltip>
    </div>
  );
};

export default QueryClearButton;
