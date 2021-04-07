import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import WithTooltip from "../tooltip/WithTooltip";
import IconButton from "../button/IconButton";

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
      <WithTooltip text={t("queryEditor.clear")}>
        <IconButton frame onClick={onClearQuery} regular icon="trash-alt" />
      </WithTooltip>
    </div>
  );
};

export default QueryClearButton;
