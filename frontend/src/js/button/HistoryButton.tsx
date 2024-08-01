import styled from "@emotion/styled";
import { faListUl } from "@fortawesome/free-solid-svg-icons";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch } from "react-redux";

import { openHistory } from "../entity-history/actions";

import IconButton from "./IconButton";

const Button = styled(IconButton)`
  white-space: nowrap;
  padding: 5px 12px;
  height: 30px;
`;

export const HistoryButton = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();

  const onClick = useCallback(() => {
    dispatch(openHistory());
  }, [dispatch]);

  return (
    <Button icon={faListUl} frame onClick={onClick}>
      {t("history.history")}
    </Button>
  );
};
