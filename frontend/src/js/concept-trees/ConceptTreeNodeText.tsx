import type { Ref } from "react";
import { tv } from "tailwind-variants";
import type { NodeIconT } from "../model/node";
import { Highlighter } from "../ui-components/Highlighter";
import { C2, textStyle } from "../ui-components/Typography";
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
    "h-5 px-[10px]",
    "text-gray-800",
    "bg-bg-50",
  ],
  variants: {
    // later wins when several are set
    disabled: {
      true: "text-gray-600",
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
    "h-4 px-1",
    "mr-[5px]",
    "rounded",
    textStyle({ size: 3, tone: "primary", strong: true }),
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
        <C2 as="span">
          {searchWords ? (
            <Highlighter searchWords={searchWords} textToHighlight={label} />
          ) : (
            label
          )}
        </C2>
        {!!description && (
          <span className={descriptionText()}>
            <C2 as="span">
              {searchWords ? (
                <Highlighter
                  searchWords={searchWords}
                  textToHighlight={description}
                />
              ) : (
                `- ${description}`
              )}
            </C2>
          </span>
        )}
      </p>
    </div>
  );
};

export default ConceptTreeNodeText;
