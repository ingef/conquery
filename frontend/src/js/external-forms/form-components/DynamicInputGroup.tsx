import { faPlus, faTimes } from "@fortawesome/free-solid-svg-icons";
import type { ReactNode } from "react";
import { tv } from "tailwind-variants";
import { Button } from "../../ui-components/Button";
import { Icon } from "../../ui-components/Icon";

interface PropsT {
  className?: string;
  label?: string;
  items: ReactNode[];
  limit: number;
  onAddClick: () => void;
  onRemoveClick: (idx: number) => void;
}

const container = tv({
  base: ["flex flex-wrap", "gap-2", "p-1"],
});

const removeButton = tv({
  base: ["absolute -top-[7px] -right-[7px]", "z-1", "rounded bg-white"],
});

const groupItem = tv({
  base: ["relative", "max-w-[200px]", "py-[2px] pr-[2px] pl-0"],
});

const DynamicInputGroup = ({
  className,
  label,
  items,
  limit,
  onRemoveClick,
  onAddClick,
}: PropsT) => {
  // 0 means "infinite"
  const limitNotReached = limit === 0 || items.length < limit;

  return (
    <div className={container({ className })}>
      {label && <span>{label}</span>}
      {items.map((item, idx) => (
        <div className={groupItem()} key={idx}>
          {item}
          {/*
            No need to display the remove button, when limit is 1.
            Assumes that this component is always nested within another container
            that allows to remove that item.

            In case you stumble accross this and you're not sure,
            you can also just delete the following constraint:
           */}
          {limit !== 1 && (
            <div className={removeButton()}>
              <Button
                intent="tertiary"
                size="sm"
                onPress={() => onRemoveClick(idx)}
              >
                <Icon icon={faTimes} />
              </Button>
            </div>
          )}
        </div>
      ))}
      {limitNotReached && (
        <Button intent="tertiary" size="sm" onPress={onAddClick}>
          <Icon icon={faPlus} />
        </Button>
      )}
    </div>
  );
};

export default DynamicInputGroup;
