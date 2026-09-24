import { FolderIcon } from "lucide-react";
import { tv } from "tailwind-variants";
import { exists } from "../../common/helpers/exists";
import { Highlighter } from "../../ui-components/Highlighter";
import { C2, textStyle } from "../../ui-components/Typography";

const root = tv({
  base: [
    "inline-flex items-center",
    "px-[7px] py-[2px]",
    "rounded",
    "cursor-pointer",
    "hover:bg-gray-50",
  ],
  variants: {
    selected: {
      true: "bg-primary-50 text-primary-500 hover:bg-primary-100",
    },
    special: { true: "italic" },
  },
});

const resultCount = tv({
  base: [
    "shrink-0",
    "inline-flex items-center justify-center",
    "h-4",
    "mr-[5px]",
    "rounded",
    textStyle({ size: 3, tone: "primary", strong: true }),
  ],
});

const Folder = ({
  className,
  resultCount: count,
  resultWords,
  folder,
  selected,
  special,
  empty,
  onClick,
}: {
  folder: string;
  resultCount: number | null;
  resultWords: string[];
  className?: string;
  selected?: boolean;
  special?: boolean;
  empty?: boolean;
  onClick: () => void;
}) => {
  return (
    // biome-ignore lint/a11y/useKeyWithClickEvents: TODO make this a button
    // biome-ignore lint/a11y/noStaticElementInteractions: see above
    <div
      key={folder}
      onClick={onClick}
      className={root({ selected, special, className })}
      title={folder}
    >
      <FolderIcon data-filled={!special} className="mr-2 text-primary-500" />
      {exists(count) && <span className={resultCount()}>{count}</span>}
      <span className="shrink-0">
        <C2 as="span">
          {!empty && resultWords.length > 0 ? (
            <Highlighter searchWords={resultWords} textToHighlight={folder} />
          ) : (
            folder
          )}
        </C2>
      </span>
    </div>
  );
};
export default Folder;
