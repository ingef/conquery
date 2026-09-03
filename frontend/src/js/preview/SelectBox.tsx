import { faCaretDown, faCaretUp } from "@fortawesome/free-solid-svg-icons";
import { type SetStateAction, useMemo, useRef, useState } from "react";
import { tv } from "tailwind-variants";
import { useClickOutside } from "../common/helpers/useClickOutside";
import { Icon } from "../ui-components/Icon";
import { Input } from "../ui-components/InputSelect/InputSelectComponents";

export interface SelectItem {
  label: string;
}

interface SelectBoxProps<T extends SelectItem> {
  items: T[];
  onChange: (item: T) => void;
  className?: string;
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
}

const root = tv({
  base: ["flex flex-col", "min-h-[30px]", "w-[20vw]"],
});

const list = tv({
  base: [
    "absolute",
    "z-1",
    "mt-[35px]",
    "flex flex-col",
    "gap-[5px]",
    "max-h-[40vh] w-[20vw]",
    "overflow-y-auto",
    "rounded",
    "bg-white",
    "shadow-[0_0_5px_rgba(0,0,0,0.2)]",
    "[clip-path:inset(0px_-8px_-8px_-8px)]",
  ],
});

const listItem = tv({
  base: ["px-[5px]", "cursor-pointer", "hover:bg-gray-50"],
});

const arrow = tv({
  base: ["mt-[5px]", "text-[17px]", "text-gray-500", "cursor-pointer"],
});

export default function SelectBox<T extends SelectItem>({
  items,
  onChange,
  className,
  isOpen,
  setIsOpen,
}: SelectBoxProps<T>) {
  const [searchTerm, setSearchTerm] = useState<string>("");
  const clickOutsideRef = useRef(null);
  useClickOutside(clickOutsideRef, () => setIsOpen(false));

  const displayedItems = useMemo(() => {
    return items.filter((item) => {
      if (searchTerm === "") {
        return true;
      }
      if (item.label === null) {
        return false;
      }
      return item.label.toLowerCase().includes(searchTerm.toLowerCase());
    });
  }, [items, searchTerm]);

  return (
    // biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a real combobox
    // biome-ignore lint/a11y/useKeyWithClickEvents: see above
    <div className={root({ className })} onClick={() => setIsOpen(!isOpen)}>
      <div className="flex flex-row">
        <Input
          className="mt-[5px] w-[190px]"
          type="text"
          placeholder=""
          value={searchTerm}
          onChange={(e: { target: { value: SetStateAction<string> } }) =>
            setSearchTerm(e.target.value)
          }
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              onChange(displayedItems[0]);
            }
          }}
          spellCheck={false}
        />
        <div className="mr-[5px]">
          <Icon icon={isOpen ? faCaretUp : faCaretDown} className={arrow()} />
        </div>
      </div>
      <div className={list()} ref={clickOutsideRef}>
        {isOpen &&
          displayedItems.map((item) => {
            return (
              // biome-ignore lint/a11y/noStaticElementInteractions: TODO make this a real listbox option
              // biome-ignore lint/a11y/useKeyWithClickEvents: see above
              <div
                className={listItem()}
                key={item.label}
                onClick={() => onChange(item)}
              >
                {item.label}
              </div>
            );
          })}
      </div>
    </div>
  );
}
