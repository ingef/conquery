import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { Fragment, useCallback, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";
import type { StateT } from "../app/reducers";
import IconButton from "../button/IconButton";
import type { DateStringMinMax } from "../common/helpers/dateHelper";
import Modal from "../modal/Modal";
import { nodeIsConceptQueryNode } from "../model/node";
import type { StandardQueryStateT } from "../standard-query-editor/queryReducer";
import type { QueryGroupType } from "../standard-query-editor/types";
import InputDateRange from "../ui-components/InputDateRange";

import {
  queryGroupModalResetAllDates,
  queryGroupModalSetDate,
} from "./actions";

const resetAll = tv({
  base: ["text-primary-500", "font-bold", "ml-5"],
});

function findGroup(query: StandardQueryStateT, andIdx: number) {
  if (!query[andIdx]) return null;

  return query[andIdx];
}

const QueryGroupModalWrap = ({
  andIdx,
  onClose,
}: {
  andIdx: number;
  onClose: () => void;
}) => {
  const group = useSelector<StateT, QueryGroupType | null>((state) =>
    findGroup(state.queryEditor.query, andIdx),
  );

  if (!group) return null;

  return <QueryGroupModal andIdx={andIdx} group={group} onClose={onClose} />;
};

const QueryGroupModal = ({
  andIdx,
  onClose,
  group,
}: {
  andIdx: number;
  onClose: () => void;
  group: QueryGroupType;
}) => {
  const { t } = useTranslation();

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

  const onResetAllDates = useCallback(
    () => dispatch(queryGroupModalResetAllDates({ andIdx })),
    [dispatch, andIdx],
  );

  const { dateRange } = group;

  const minDate = dateRange ? dateRange.min || null : null;
  const maxDate = dateRange ? dateRange.max || null : null;
  const hasActiveDate = !!(minDate || maxDate);

  const labelSuffix = useMemo(() => {
    return hasActiveDate ? (
      <IconButton
        className={resetAll()}
        bare
        onClick={onResetAllDates}
        icon={faUndo}
      >
        {t("queryNodeEditor.reset")}
      </IconButton>
    ) : null;
  }, [t, hasActiveDate, onResetAllDates]);

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={t("queryGroupModal.explanation")}
    >
      <div className="block mb-[15px] max-w-[450px]">
        <span className="pr-[5px]" key={-1}>
          {t("queryGroupModal.headlineStart")}
        </span>
        {group.elements.map((node, i) => (
          <Fragment key={`${i}-headline`}>
            <span className="pr-[5px]">
              {node.label ||
                (nodeIsConceptQueryNode(node) ? node.ids[0] : node.id)}
            </span>
            {i !== group.elements.length - 1 && (
              <span key={`${i}-comma`}>, </span>
            )}
          </Fragment>
        ))}
      </div>
      <InputDateRange
        large
        inline
        autoFocus
        label={t("queryGroupModal.dateRange")}
        labelSuffix={labelSuffix}
        onChange={onSetDate}
        value={{
          min: minDate,
          max: maxDate,
        }}
      />
    </Modal>
  );
};

export default QueryGroupModalWrap;
