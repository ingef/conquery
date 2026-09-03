import {
  faCaretDown,
  faCaretRight,
  type IconDefinition,
} from "@fortawesome/free-solid-svg-icons";
import type { Ref } from "react";
import { tv } from "tailwind-variants";
import { Highlighter } from "../common/components/Highlighter";

import { Icon } from "../ui-components/Icon";

// Root with transparent background.
// relative: needed to fix a drag & drop issue in Safari
const root = tv({
  base: ["relative", "flex", "cursor-pointer", "my-[2px]", "pr-[15px]"],
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

const caretIconContainer = tv({
  base: ["inline-block", "w-[14px]", "shrink-0"],
});

const folderIconContainer = tv({
  base: ["inline-block", "w-5", "shrink-0"],
});

const dashIconContainer = tv({
  base: ["flex items-center", "w-[34px]", "shrink-0", "pl-[14px]", "text-left"],
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
    "leading-none",
    "text-xs",
    "rounded",
    "text-primary-500",
    "font-bold",
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
  hasChildren,

  onClick,
}: {
  ref?: Ref<HTMLDivElement>;

  label: string;
  depth: number;
  icon: IconDefinition;

  className?: string;
  description?: string;
  resultCount?: number | null;
  searchWords?: string[] | null;
  isOpen?: boolean;
  hasChildren?: boolean;
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
        {hasChildren && (
          <>
            <span className={caretIconContainer()}>
              <Icon
                icon={isOpen ? faCaretDown : faCaretRight}
                className={[
                  "text-primary-500",
                  disabled ? "text-gray-400 cursor-not-allowed" : undefined,
                ]}
              />
            </span>
            <span className={folderIconContainer()}>
              <Icon
                icon={icon}
                className={[
                  "text-primary-500",
                  disabled ? "text-gray-400 cursor-not-allowed" : undefined,
                ]}
              />
            </span>
          </>
        )}
        {!hasChildren && (
          <span className={dashIconContainer()}>
            <Icon
              icon={icon}
              className={[
                "text-primary-500",
                disabled ? "text-gray-400 cursor-not-allowed" : undefined,
              ]}
            />
          </span>
        )}
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
