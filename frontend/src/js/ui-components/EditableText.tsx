import { faPen } from "@fortawesome/free-solid-svg-icons";
import { tv } from "tailwind-variants";
import { Highlighter } from "../common/components/Highlighter";
import HighlightableLabel from "../highlightable-label/HighlightableLabel";
import { Button } from "./Button";
import EditableTextForm from "./EditableTextForm";
import { Icon } from "./Icon";
import { Tooltip, TooltipTrigger } from "./Tooltip";

const editButton = tv({
  variants: {
    large: {
      true: "mr-[10px]",
      false: "mr-2",
    },
  },
});

const text = tv({
  base: "flex flex-row items-center",
});

const label = tv({
  base: ["overflow-hidden", "text-ellipsis", "whitespace-nowrap"],
});

const EditableText = ({
  className,
  loading,
  editing,
  text: textValue,
  tooltip,
  large,
  saveOnClickoutside,
  isHighlighted,
  highlightedWords,
  selectTextOnMount,
  onSubmit,
  onToggleEdit,
}: {
  className?: string;
  loading?: boolean;
  editing: boolean;
  text: string;
  tooltip?: string;
  large?: boolean;
  saveOnClickoutside?: boolean;
  isHighlighted?: boolean;
  highlightedWords?: string[];
  selectTextOnMount?: boolean;
  onSubmit: (text: string) => void;
  onToggleEdit: () => void;
}) => {
  return editing ? (
    <EditableTextForm
      className={className}
      loading={loading}
      text={textValue}
      selectTextOnMount={selectTextOnMount}
      saveOnClickoutside={saveOnClickoutside}
      onSubmit={onSubmit}
      onCancel={onToggleEdit}
    />
  ) : (
    <div className={text({ className })}>
      <span className={editButton({ large: !!large })}>
        <TooltipTrigger>
          <Button
            aria-label={tooltip}
            intent="tertiary"
            size={large ? "md" : "sm"}
            onPress={onToggleEdit}
          >
            <Icon icon={faPen} />
          </Button>
          <Tooltip>{tooltip}</Tooltip>
        </TooltipTrigger>
      </span>
      <HighlightableLabel className={label()} isHighlighted={isHighlighted}>
        {highlightedWords && highlightedWords.length > 0 ? (
          <Highlighter
            searchWords={highlightedWords}
            textToHighlight={textValue}
          />
        ) : (
          textValue
        )}
      </HighlightableLabel>
    </div>
  );
};

export default EditableText;
