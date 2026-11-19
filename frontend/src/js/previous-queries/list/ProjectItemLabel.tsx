import { useTranslation } from "react-i18next";
import { tv } from "tailwind-variants";
import EditableText from "../../ui-components/EditableText";
import { Highlighter } from "../../ui-components/Highlighter";

const labelText = tv({
  base: ["whitespace-nowrap", "overflow-hidden text-ellipsis"],
});

const ProjectItemLabel = ({
  mayEdit,
  label,
  highlightedWords,
  onSubmit,
  isEditing,
  setIsEditing,
}: {
  mayEdit?: boolean;
  label: string;
  highlightedWords: string[];
  onSubmit: (text: string) => void;
  isEditing: boolean;
  setIsEditing: (value: boolean) => void;
}) => {
  const { t } = useTranslation();

  return mayEdit ? (
    <EditableText
      size="sm"
      text={label}
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
