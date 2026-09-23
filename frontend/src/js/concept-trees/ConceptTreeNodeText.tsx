import type { Ref } from "react";
import { tv } from "tailwind-variants";
import type { NodeIconT } from "../model/node";
import { Highlighter } from "../ui-components/Highlighter";
import { NodeIcon } from "./NodeIcon";

// Root with transparent background.
// relative: needed to fix a drag & drop issue in Safari
// isolate: keeps other layers out of Chrome's drag image of the row
const root = tv({
  base: ["relative isolate", "flex", "cursor-pointer", "my-[2px]", "pr-[15px]"],
});

const text = tv({
  base: [
    "inline-flex flex-row flex-nowrap items-center",
    "select-none",
    "rounded",
    "border border-transparent",
    "px-[10px]",
    "leading-[18px]",
    "text-gray-800",
    "bg-bg-50",
  ],
  variants: {
    // later wins when several are set
    disabled: {
      true: "text-gray-500",
      false: "hover:border-primary-200",
    },
    red: { true: "text-red" },
    isOpen: { true: "bg-gray-50" },
  },
});

const nodeIcon = tv({
  base: "mr-1 text-primary-500",
  variants: {
    disabled: { true: "text-gray-400 cursor-not-allowed" },
  },
});

const descriptionText = tv({
  base: ["inline-block", "shrink-0", "pl-[3px]"],
});

const resultsNumber = tv({
  base: [
    "inline-flex items-center justify-center",
    "shrink-0",
    "px-1 py-[2px]",
    "mr-[5px]",
    "text-xs",
    "leading-none",
    "rounded",
    "text-primary-500",
    "font-medium",
  ],
});

const ConceptTreeNodeText = ({
  ref,
  label,
  description,
  icon,
  resultCount,
  searchWords,
  className,
  depth,

  isOpen,
  red,
  disabled,

  onClick,
}: {
  ref?: Ref<HTMLDivElement>;

  label: string;
  depth: number;
  icon: NodeIconT;

  className?: string;
  description?: string;
  resultCount?: number | null;
  searchWords?: string[] | null;
  isOpen?: boolean;
  red?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}) => {
  return (
    <div
      ref={ref}
      className={root({ className })}
      style={{ paddingLeft: depth * 15 }}
    >
      {/* biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button */}
      <p
        className={text({ disabled: !!disabled, red, isOpen })}
        onClick={onClick}
      >
        <NodeIcon icon={icon} className={nodeIcon({ disabled: !!disabled })} />
        {resultCount && <span className={resultsNumber()}>{resultCount}</span>}
        <span>
          {searchWords ? (
            <Highlighter searchWords={searchWords} textToHighlight={label} />
          ) : (
            label
          )}
        </span>
        {!!description && (
          <span className={descriptionText()}>
            {searchWords ? (
              <Highlighter
                searchWords={searchWords}
                textToHighlight={description}
              />
            ) : (
              `- ${description}`
            )}
          </span>
        )}
      </p>
    </div>
  );
};

export default ConceptTreeNodeText;
