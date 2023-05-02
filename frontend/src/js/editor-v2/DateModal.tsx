import styled from "@emotion/styled";
import { faUndo } from "@fortawesome/free-solid-svg-icons";
import { useState, useCallback, useMemo } from "react";
import { useHotkeys } from "react-hotkeys-hook";
import { useTranslation } from "react-i18next";

import { DateRangeT } from "../api/types";
import IconButton from "../button/IconButton";
import { DateStringMinMax } from "../common/helpers/dateHelper";
import Modal from "../modal/Modal";
import InputDateRange from "../ui-components/InputDateRange";

import { Tree } from "./types";

export const useDateEditing = ({
  enabled,
  selectedNode,
}: {
  enabled: boolean;
  selectedNode: Tree | undefined;
}) => {
  const [showModal, setShowModal] = useState(false);

  const onClose = useCallback(() => setShowModal(false), []);
  const onOpen = useCallback(() => {
    if (!enabled) return;
    if (!selectedNode) return;

    setShowModal(true);
  }, [enabled, selectedNode]);

  useHotkeys("d", onOpen, [onOpen], {
    preventDefault: true,
  });

  const headline = useMemo(() => {
    if (!selectedNode) return "";

    return (
      selectedNode.data?.label ||
      (selectedNode.children?.items || []).map((c) => c.data?.label).join(" ")
    );
  }, [selectedNode]);

  return {
    showModal,
    headline,
    onClose,
    onOpen,
  };
};

const ResetAll = styled(IconButton)`
  color: ${({ theme }) => theme.col.blueGrayDark};
  font-weight: 700;
  margin-left: 20px;
`;

export const DateModal = ({
  onClose,
  dateRange = {},
  headline,
  setDateRange,
  onResetDates,
}: {
  onClose: () => void;
  dateRange?: DateRangeT;
  headline: string;
  setDateRange: (range: DateRangeT) => void;
  onResetDates: () => void;
}) => {
  const { t } = useTranslation();

  useHotkeys("esc", onClose, [onClose]);

  const minDate = dateRange ? dateRange.min || null : null;
  const maxDate = dateRange ? dateRange.max || null : null;
  const hasActiveDate = !!(minDate || maxDate);

  const labelSuffix = useMemo(() => {
    return hasActiveDate ? (
      <ResetAll bare onClick={onResetDates} icon={faUndo}>
        {t("queryNodeEditor.reset")}
      </ResetAll>
    ) : null;
  }, [t, hasActiveDate, onResetDates]);

  const onChange = useCallback(
    (date: DateStringMinMax) => {
      setDateRange({
        min: date.min || undefined,
        max: date.max || undefined,
      });
    },
    [setDateRange],
  );

  return (
    <Modal
      onClose={onClose}
      doneButton
      headline={t("queryGroupModal.explanation")}
    >
      <div>{headline}</div>
      <InputDateRange
        large
        inline
        autoFocus
        label={t("queryGroupModal.dateRange")}
        labelSuffix={labelSuffix}
        onChange={onChange}
        value={{
          min: minDate,
          max: maxDate,
        }}
      />
    </Modal>
  );
};
