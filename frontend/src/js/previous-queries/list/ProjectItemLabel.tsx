import styled from "@emotion/styled";
import { FC } from "react";
import Highlighter from "react-highlight-words";
import { useTranslation } from "react-i18next";
import { useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import EditableText from "../../ui-components/EditableText";

const Text = styled("div")`
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

const useHighlightedWords = () => {
  return useSelector<StateT, string[]>(
    (state) => state.projectItemsSearch.words,
  );
};

interface PropsT {
  mayEdit?: boolean;
  label: string;
  selectTextOnMount: boolean;
  loading?: boolean;
  onSubmit: (text: string) => void;
  isEditing: boolean;
  setIsEditing: (value: boolean) => void;
}

const ProjectItemLabel: FC<PropsT> = ({
  mayEdit,
  loading,
  selectTextOnMount,
  label,
  onSubmit,
  isEditing,
  setIsEditing,
}) => {
  const highlightedWords = useHighlightedWords();
  const { t } = useTranslation();

  return mayEdit ? (
    <SxEditableText
      loading={loading}
      text={label}
      selectTextOnMount={selectTextOnMount}
      editing={isEditing}
      onSubmit={onSubmit}
      onToggleEdit={() => setIsEditing(!isEditing)}
      highlightedWords={highlightedWords}
      tooltip={t("common.edit")}
    />
  ) : (
    <Text>
      {highlightedWords.length > 0 ? (
        <Highlighter
          searchWords={highlightedWords}
          autoEscape
          textToHighlight={label}
        />
      ) : (
        label
      )}
    </Text>
  );
};

export default ProjectItemLabel;
