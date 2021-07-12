import styled from "@emotion/styled";
import { useEffect, useState } from "react";

import IconButton from "../button/IconButton";
import { exists } from "../common/helpers/exists";
import BaseInput from "../form-components/BaseInput";

const InputContainer = styled("div")`
  flex-grow: 1;
  position: relative;
`;

const SxBaseInput = styled(BaseInput)`
  width: 100%;
  input {
    padding-right: 60px;
    width: 100%;
    &::placeholder {
      color: ${({ theme }) => theme.col.grayMediumLight};
      opacity: 1;
    }
  }
`;

const Right = styled("div")`
  position: absolute;
  top: 0px;
  right: 30px;
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 34px;
`;

const StyledIconButton = styled(IconButton)`
  padding: 8px 10px;
  color: ${({ theme }) => theme.col.gray};
`;

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
    <InputContainer className={className}>
      <SxBaseInput
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
        <Right>
          <StyledIconButton
            icon="search"
            aria-hidden="true"
            onClick={() => onSearch(localSearchTerm)}
          />
        </Right>
      )}
    </InputContainer>
  );
};

export default SearchBar;
