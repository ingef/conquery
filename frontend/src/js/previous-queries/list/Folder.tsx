import { faFolder as faFolderRegular } from "@fortawesome/free-regular-svg-icons";
import { faFolder } from "@fortawesome/free-solid-svg-icons";
import { tv } from "tailwind-variants";
import { Highlighter } from "../../common/components/Highlighter";

import { exists } from "../../common/helpers/exists";
import { Icon } from "../../ui-components/Icon";

const root = tv({
  base: [
    "inline-flex items-center",
    "px-[7px] py-[2px]",
    "rounded",
    "text-sm",
    "cursor-pointer",
    "bg-transparent hover:bg-primary-50",
  ],
  variants: {
    active: { true: "bg-gray-100" },
    special: { true: "italic" },
  },
});

const resultCount = tv({
  base: [
    "shrink-0",
    "inline-flex items-center justify-center",
    "leading-none",
    "py-[2px]",
    "mr-[5px]",
    "text-xs",
    "rounded",
    "text-primary-500",
    "font-bold",
  ],
});

const Folder = ({
  className,
  resultCount: count,
  resultWords,
  folder,
  active,
  special,
  empty,
  onClick,
}: {
  folder: string;
  resultCount: number | null;
  resultWords: string[];
  className?: string;
  active?: boolean;
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
      className={root({ active, special, className })}
      title={folder}
    >
      <Icon
        icon={special ? faFolderRegular : faFolder}
        className="mr-2 text-primary-500"
      />
      {exists(count) && <span className={resultCount()}>{count}</span>}
      <div className="shrink-0 text-gray-800">
        {!empty && resultWords.length > 0 ? (
          <Highlighter searchWords={resultWords} textToHighlight={folder} />
        ) : (
          folder
        )}
      </div>
    </div>
  );
};
export default Folder;
