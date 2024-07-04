import styled from "@emotion/styled";
import { memo, useEffect, useState } from "react";

import { exists } from "../common/helpers/exists";
import { useDebounce } from "../common/helpers/useDebounce";
import BaseInput from "../ui-components/BaseInput";

const SxBaseInput = styled(BaseInput)`
  width: 100%;
  input {
    height: 34px;
    width: 100%;
    &::placeholder {
      color: ${({ theme }) => theme.col.grayMediumLight};
      opacity: 1;
    }
  }
`;

const SearchBar = ({
  searchTerm,
  placeholder,
  onSearch,
  onClear,
}: {
  searchTerm: string | null;
  placeholder: string;
  onSearch: (value: string) => void;
  onClear: () => void;
}) => {
  const [localSearchTerm, setLocalSearchTerm] = useState<string | null>(null);

  useDebounce(
    () => {
      if (exists(localSearchTerm)) onSearch(localSearchTerm);
    },
    500,
    [localSearchTerm],
  );

  useEffect(() => {
    setLocalSearchTerm(searchTerm);
  }, [searchTerm]);

  return (
    <div className="flex-grow">
      <SxBaseInput
        inputType="text"
        placeholder={placeholder}
        value={localSearchTerm || ""}
        onChange={(value) => {
          if (!exists(value)) onClear();

          setLocalSearchTerm(value as string | null);
        }}
      />
    </div>
  );
};

export default memo(SearchBar);
