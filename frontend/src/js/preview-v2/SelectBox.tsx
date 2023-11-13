import styled from "@emotion/styled";
import { SetStateAction, useMemo, useRef, useState } from "react";

import { Input } from "../ui-components/InputSelect/InputSelectComponents";
import FaIcon from "../icon/FaIcon";
import { faCaretDown, faCaretUp } from "@fortawesome/free-solid-svg-icons";
import { useClickOutside } from "../common/helpers/useClickOutside";

interface SelectItem {
  label: string;
  name: string; // Used as key
}

interface SelectBoxProps<T extends SelectItem> {
  items: T[];
  selected: T[];
  onChange: (item: T) => void;
  className?: string;
}

const Root = styled("div")`
  display: flex;
  min-height: 30px;
  flex-direction: column;
`;

const InputContainer = styled("div")`
  display: flex;
  flex-direction: row;
`;

// TODO combine Margin
const DropDownContainer = styled("div")`
  position: absolute;
  z-index: 1;
  margin: 5px;
  margin-top: 30px;
  background-color: white;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.2);
  clip-path: inset(0px -8px -8px -8px);
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-height: 40vh;
  overflow-y: auto;
  width: 200px;
`;

const SxInput = styled(Input)`
  margin-top: 5px;
  width: 190px;
`;
const ArrowContainer = styled("div")`
  margin-right: 5px;
`;
const SxArrow = styled(FaIcon)`
  margin-top: 5px;
  color: ${({ theme }) => theme.col.gray};
  font-size: 17px;
  cursor: pointer;
`;

export default function SelectBox<T extends SelectItem>({
  items,
  selected,
  onChange,
  className,
}: SelectBoxProps<T>) {
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const clickOutsideRef = useRef(null);
  useClickOutside(clickOutsideRef, () => setIsOpen(false));

  const displayedItems = useMemo(() => {
    return items.filter((item) => item.name.includes(searchTerm));
  }, [items, searchTerm]);

  return (
    <Root className={className} onClick={() => setIsOpen(!isOpen)} ref={clickOutsideRef}>
      <InputContainer>
        <SxInput
          type="text"
          placeholder=""
          value={searchTerm}
          onChange={(e: { target: { value: SetStateAction<string> } }) =>
            setSearchTerm(e.target.value)
          }
          spellCheck={false}
        />
        <ArrowContainer>
          <SxArrow icon={isOpen ? faCaretUp : faCaretDown}/>
        </ArrowContainer>
      </InputContainer>
      {/*  use Menu container?! */}
      <DropDownContainer>
        {isOpen && (
            displayedItems.map((item) => {
              return (<div key={item.name}>{item.label}</div>)
            })    
          )  
        }
      </DropDownContainer>
    </Root>
  );
}
