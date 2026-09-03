import { faSearch } from "@fortawesome/free-solid-svg-icons";
import { memo, useEffect, useState } from "react";
import { tv } from "tailwind-variants";

import IconButton from "../button/IconButton";
import { exists } from "../common/helpers/exists";
import BaseInput from "../ui-components/BaseInput";

const inputContainer = tv({ base: ["relative", "grow"] });

const baseInput = tv({
  base: [
    "w-full",
    "[&_input]:h-[34px] [&_input]:w-full",
    "[&_input]:pr-[60px]",
    "[&_input]:placeholder:text-gray-400",
    "[&_input]:placeholder:opacity-100",
  ],
});

const right = tv({
  base: [
    "absolute top-0 right-[30px]",
    "flex flex-row items-center",
    "h-[34px]",
  ],
});

const searchButton = tv({ base: ["px-[10px] py-2", "text-gray-500"] });

interface Props {
  className?: string;
  searchTerm: string | null;
  placeholder: string;
  onSearch: (value: string) => void;
  onClear: () => void;
}

const SearchBar = ({
  className,
  searchTerm,
  placeholder,
  onSearch,
  onClear,
}: Props) => {
  const [localSearchTerm, setLocalSearchTerm] = useState<string | null>(null);

  useEffect(() => {
    setLocalSearchTerm(searchTerm);
  }, [searchTerm]);

  return (
    <div className={inputContainer({ className })}>
      <BaseInput
        className={baseInput()}
        inputType="text"
        placeholder={placeholder}
        value={localSearchTerm || ""}
        onChange={(value) => {
          if (!exists(value)) onClear();

          setLocalSearchTerm(value as string | null);
        }}
        inputProps={{
          onKeyPress: (e) => {
            return e.key === "Enter" && exists(localSearchTerm)
              ? onSearch(localSearchTerm)
              : null;
          },
        }}
      />
      {exists(localSearchTerm) && (
        <div className={right()}>
          <IconButton
            className={searchButton()}
            icon={faSearch}
            aria-label={placeholder}
            onClick={() => onSearch(localSearchTerm)}
          />
        </div>
      )}
    </div>
  );
};

export default memo(SearchBar);
