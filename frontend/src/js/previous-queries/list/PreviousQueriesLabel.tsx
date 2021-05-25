import styled from "@emotion/styled";
import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import EditableText from "../../form-components/EditableText";
import HighlightableLabel from "../../highlightable-label/HighlightableLabel";

const SxSelectableLabel = styled(HighlightableLabel)`
  display: block;
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;
const SxEditableText = styled(EditableText)`
  font-weight: 400;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const labelContainsAnySearch = (label: string, searches: string[]) =>
  searches.some(
    (search) => label.toLowerCase().indexOf(search.toLowerCase()) !== -1,
  );

const useIsHighlightedLabel = (label: string) => {
  const previousQueriesSearch = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch,
  );

  return labelContainsAnySearch(label, previousQueriesSearch);
};

interface PropsT {
  mayEditQuery?: boolean;
  label: string;
  selectTextOnMount: boolean;
  loading: boolean;
  onSubmit: (text: string) => void;
  isEditing: boolean;
  setIsEditing: (value: boolean) => void;
}

const PreviousQueriesLabel: FC<PropsT> = ({
  mayEditQuery,
  loading,
  selectTextOnMount,
  label,
  onSubmit,
  isEditing,
  setIsEditing,
}) => {
  const isHighlightedLabel = useIsHighlightedLabel(label);
  const { t } = useTranslation();

  return mayEditQuery ? (
    <SxEditableText
      loading={loading}
      text={label}
      selectTextOnMount={selectTextOnMount}
      editing={isEditing}
      onSubmit={onSubmit}
      onToggleEdit={() => setIsEditing(!isEditing)}
      isHighlighted={isHighlightedLabel}
      tooltip={t("common.edit")}
    />
  ) : (
    <SxSelectableLabel label={label} isHighlighted={isHighlightedLabel} />
  );
};

export default PreviousQueriesLabel;
