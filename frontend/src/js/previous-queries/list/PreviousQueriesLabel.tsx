import React, { FC } from "react";
import styled from "@emotion/styled";
import EditableText from "js/form-components/EditableText";
import HighlightableLabel from "js/highlightable-label/HighlightableLabel";
import { StateT } from "app-types";
import { useSelector } from "react-redux";

const StyledSelectableLabel = styled(HighlightableLabel)`
  margin: 0;
  font-weight: 400;
  word-break: break-word;
`;
const StyledEditableText = styled(EditableText)`
  margin: 0;
  font-weight: 400;
  word-break: break-word;
`;

const labelContainsAnySearch = (label: string, searches: string[]) =>
  searches.some(
    (search) => label.toLowerCase().indexOf(search.toLowerCase()) !== -1
  );

const useIsHighlightedLabel = (label: string) => {
  const previousQueriesSearch = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch
  );

  return labelContainsAnySearch(label, previousQueriesSearch);
};

interface PropsT {
  mayEditQuery: boolean;
  label: string;
  selectTextOnMount: boolean;
  editing: boolean;
  loading: boolean;
  onToggleEdit: () => void;
  onSubmit: (text: string) => void;
}

const PreviousQueriesLabel: FC<PropsT> = ({
  mayEditQuery,
  loading,
  editing,
  selectTextOnMount,
  label,
  onSubmit,
  onToggleEdit,
}) => {
  const isHighlightedLabel = useIsHighlightedLabel(label);

  return mayEditQuery ? (
    <StyledEditableText
      loading={loading}
      text={label}
      selectTextOnMount={selectTextOnMount}
      editing={editing}
      onSubmit={onSubmit}
      onToggleEdit={onToggleEdit}
      isHighlighted={isHighlightedLabel}
    />
  ) : (
    <StyledSelectableLabel label={label} isHighlighted={isHighlightedLabel} />
  );
};

export default PreviousQueriesLabel;
