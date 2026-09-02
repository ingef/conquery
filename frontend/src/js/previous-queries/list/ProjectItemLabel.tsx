import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import { Highlighter } from "../../common/components/Highlighter";

import EditableText from "../../ui-components/EditableText";

const labelText = tv({
  base: ["font-normal", "whitespace-nowrap", "overflow-hidden text-ellipsis"],
});

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
    <EditableText
      className={labelText()}
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
    <div className={labelText()}>
      {highlightedWords.length > 0 ? (
        <Highlighter searchWords={highlightedWords} textToHighlight={label} />
      ) : (
        label
      )}
    </div>
  );
};

export default ProjectItemLabel;
