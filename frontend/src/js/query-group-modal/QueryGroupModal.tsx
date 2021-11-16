import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC, Fragment } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import IconButton from "../button/IconButton";
import { DateStringMinMax } from "../common/helpers";
import Modal from "../modal/Modal";
import { nodeIsConceptQueryNode } from "../model/node";
import { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import { QueryGroupType } from "../standard-query-editor/types";
import InputDateRange from "../ui-components/InputDateRange";

import {
  queryGroupModalSetDate,
  queryGroupModalResetAllDates,
} from "./actions";

const HeadlinePart = styled("span")`
  padding: 0 5px 0 0;
`;

const Elements = styled("div")`
  display: block;
  margin: 0 0 15px;
  max-width: 450px;
`;

const ResetAll = styled(IconButton)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin-left: 20px;
`;

function findGroup(query: StandardQueryStateT, andIdx: number) {
  if (!query[andIdx]) return null;

  return query[andIdx];
}

interface PropsT {
  andIdx: number;
  onClose: () => void;
}

const QueryGroupModal: FC<PropsT> = ({ andIdx, onClose }) => {
  const { t } = useTranslation();

  const group = useSelector<StateT, QueryGroupType | null>((state) =>
    findGroup(state.queryEditor.query, andIdx),
  );

  const dispatch = useDispatch();

  const onSetDate = (date: DateStringMinMax) => {
    dispatch(
      queryGroupModalSetDate({
        andIdx,
        date: {
          min: date.min || undefined,
          max: date.max || undefined,
        },
      }),
    );
  };
  const onResetAllDates = () =>
    dispatch(queryGroupModalResetAllDates({ andIdx }));

  if (!group) return null;

  const { dateRange } = group;

  const minDate = dateRange ? dateRange.min || null : null;
  const maxDate = dateRange ? dateRange.max || null : null;
  const hasActiveDate = !!(minDate || maxDate);

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={t("queryGroupModal.explanation")}
    >
      <Elements>
        <HeadlinePart key={-1}>
          {t("queryGroupModal.headlineStart")}
        </HeadlinePart>
        {group.elements.map((node, i) => (
          <Fragment key={i + "-headline"}>
            <HeadlinePart>
              {node.label ||
                (nodeIsConceptQueryNode(node) ? node.ids[0] : node.id)}
            </HeadlinePart>
            {i !== group.elements.length - 1 && (
              <span key={i + "-comma"}>, </span>
            )}
          </Fragment>
        ))}
      </Elements>
      <InputDateRange
        large
        inline
        autoFocus
        label={t("queryGroupModal.dateRange")}
        labelSuffix={
          <>
            {hasActiveDate && (
              <ResetAll bare onClick={onResetAllDates} icon="undo">
                {t("queryNodeEditor.reset")}
              </ResetAll>
            )}
          </>
        }
        onChange={onSetDate}
        value={{
          min: minDate,
          max: maxDate,
        }}
      />
    </Modal>
  );
};

export default QueryGroupModal;
