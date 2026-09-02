import { faPen } from "@fortawesome/free-solid-svg-icons";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";
import { Highlighter } from "../common/components/Highlighter";
import HighlightableLabel from "../highlightable-label/HighlightableLabel";
import EditableTextForm from "./EditableTextForm";
import { Tooltip, TooltipTrigger } from "./Tooltip";

const editButton = tv({
  base: ["px-0 py-[2px]"],
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
      <TooltipTrigger>
        <IconButton
          className={editButton({ large: !!large })}
          bare
          icon={faPen}
          onClick={onToggleEdit}
          small
          large={large}
        />
        <Tooltip>{tooltip}</Tooltip>
      </TooltipTrigger>
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
