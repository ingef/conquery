import styled from "@emotion/styled";
import Highlighter from "react-highlight-words";
import { useTranslation } from "react-i18next";

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

const ProjectItemLabel = ({
  mayEdit,
  loading,
  selectTextOnMount,
  label,
  highlightedWords,
  onSubmit,
  isEditing,
  setIsEditing,
}: {
  mayEdit?: boolean;
  label: string;
  highlightedWords: string[];
  selectTextOnMount: boolean;
  loading?: boolean;
  onSubmit: (text: string) => void;
  isEditing: boolean;
  setIsEditing: (value: boolean) => void;
}) => {
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
