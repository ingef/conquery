import { PencilIcon } from "lucide-react";
import { tv } from "tailwind-variants";
import HighlightableLabel from "../highlightable-label/HighlightableLabel";
import { Button } from "./Button";
import EditableTextForm from "./EditableTextForm";
import { Highlighter } from "./Highlighter";
import { Tooltip, TooltipTrigger } from "./Tooltip";
import { C2 } from "./Typography";

// a flex wrapper, so the button does not sit on a text baseline and grow the row
const editButton = tv({
  base: "flex shrink-0",
  variants: {
    size: {
      sm: "mr-2",
      md: "mr-[10px]",
    },
  },
});

const text = tv({
  base: "flex min-w-0 flex-row items-center",
});

const label = tv({
  base: ["overflow-hidden", "text-ellipsis", "whitespace-nowrap"],
});

const EditableText = ({
  editing,
  text: textValue,
  tooltip,
  size = "md",
  isHighlighted,
  highlightedWords,
  onSubmit,
  onToggleEdit,
}: {
  editing: boolean;
  text: string;
  tooltip?: string;
  /** the button sizes; the field matches their height */
  size?: "sm" | "md";
  isHighlighted?: boolean;
  highlightedWords?: string[];
  onSubmit: (text: string) => void;
  onToggleEdit: () => void;
}) => {
  return editing ? (
    <EditableTextForm
      size={size}
      label={tooltip}
      text={textValue}
      onSubmit={(newText) => {
        onSubmit(newText);
        onToggleEdit();
      }}
      onCancel={onToggleEdit}
    />
  ) : (
    <div className={text()}>
      <span className={editButton({ size })}>
        <TooltipTrigger>
          <Button
            aria-label={tooltip}
            intent="tertiary"
            size={size}
            onPress={onToggleEdit}
          >
            <PencilIcon />
          </Button>
          <Tooltip>{tooltip}</Tooltip>
        </TooltipTrigger>
      </span>
      <HighlightableLabel className={label()} isHighlighted={isHighlighted}>
        <C2 as="span">
          {highlightedWords && highlightedWords.length > 0 ? (
            <Highlighter
              searchWords={highlightedWords}
              textToHighlight={textValue}
            />
          ) : (
            textValue
          )}
        </C2>
      </HighlightableLabel>
    </div>
  );
};

export default EditableText;
