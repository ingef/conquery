import styled from "@emotion/styled";
import { SetStateAction, useMemo, useRef, useState } from "react";

import { Input } from "../ui-components/InputSelect/InputSelectComponents";
import FaIcon from "../icon/FaIcon";
import { faCaretDown, faCaretUp, faCheck } from "@fortawesome/free-solid-svg-icons";
import { useClickOutside } from "../common/helpers/useClickOutside";

export interface SelectItem {
  label: string;
  name: string; // Used as key
}

interface SelectBoxProps<T extends SelectItem> {
  items: T[];
  selected: T[];
  onChange: (item: T[]) => void;
  className?: string;
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
}

const Root = styled("div")`
  display: flex;
  min-height: 30px;
  flex-direction: column;
  width: 20vw;
`;

const InputContainer = styled("div")`
  display: flex;
  flex-direction: row;
`;

// TODO combine Margin
const List = styled("div")`
  position: absolute;
  z-index: 1;
  margin-top: 35px;
  background-color: white;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.2);
  border-radius: ${({ theme }) => theme.borderRadius};
  clip-path: inset(0px -8px -8px -8px);
  display: flex;
  flex-direction: column;
  gap: 5px;
  max-height: 40vh;
  overflow-y: auto;
  width: 20vw;
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

const SxCheckmark = styled(FaIcon)<{showCheckmark?: boolean}>`
  color: ${({ theme }) => theme.col.gray};
  visibility: ${({ showCheckmark }) => showCheckmark ? "visible" : "hidden"};
  margin: 0 5px;
`;

export default function SelectBox<T extends SelectItem>({
  items,
  selected,
  onChange,
  className,
  isOpen,
  setIsOpen,
}: SelectBoxProps<T>) {
  const [searchTerm, setSearchTerm] = useState<string>("");
  const clickOutsideRef = useRef(null);
  useClickOutside(clickOutsideRef, () => setIsOpen(false));

  const displayedItems = useMemo(() => {
    return items.filter((item) => item.label.toLowerCase().includes(searchTerm.toLowerCase()));
  }, [items, searchTerm]);

  return (
    <Root className={className} onClick={() => setIsOpen(true)} >
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
      <List ref={clickOutsideRef}>
        {isOpen && (
            displayedItems.map((item) => {
              return (<div key={item.name}
                onClick={() => {
                  let newSelected = [...selected];
                  if(selected.includes(item)) {
                    newSelected = newSelected.filter((i) => i !== item);
                  } else {
                    newSelected.push(item);
                  }
                  onChange(newSelected);
                }}
              >
                <SxCheckmark icon={faCheck} showCheckmark={ selected.some((selected) => selected.name === item.name) } />
                {item.label}
              </div>)
            })    
          )  
        }
      </List>
    </Root>
  );
}
