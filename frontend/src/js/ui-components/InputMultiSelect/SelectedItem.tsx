import { faTimes } from "@fortawesome/free-solid-svg-icons";
import { memo, type Ref } from "react";
import ReactMarkdown from "react-markdown";
import { tv } from "tailwind-variants";
import type { SelectOptionT } from "../../api/types";
import { Button } from "../Button";
import { Icon } from "../Icon";

const container = tv({
  base: [
    "flex items-center",
    "rounded",
    "bg-gray-50",
    "px-[5px] py-0",
    "text-sm",
    "text-gray-800",
    "shadow-[0.5px_0.5px_1px_0_rgb(0_0_0/20%),inset_0_0_0_1px_#ccc]",
    // to style react-markdown
    "[&_p]:m-0",
  ],
});

const SelectedItem = ({
  ref,
  index,
  item,
  disabled,
  removeSelectedItem,
  getSelectedItemProps,
}: {
  ref?: Ref<HTMLDivElement>;

  active?: boolean;
  disabled?: boolean;
  item: SelectOptionT;
  index: number;
  getSelectedItemProps: (props: {
    selectedItem: SelectOptionT;
    index: number;
  }) => object;
  removeSelectedItem: (item: SelectOptionT) => void;
}) => {
  const label = item.selectedLabel || item.label || item.value;

  const selectedItemProps = getSelectedItemProps({
    selectedItem: item,
    index,
  });

  return (
    <div className={container()} ref={ref} {...selectedItemProps}>
      <ReactMarkdown>{String(label)}</ReactMarkdown>
      <Button
        size="sm"
        intent="tertiary"
        isDisabled={disabled}
        onPress={() => {
          // otherwise the click handler on the Container overrides this
          removeSelectedItem(item);
        }}
      >
        <Icon icon={faTimes} />
      </Button>
    </div>
  );
};

export default memo(SelectedItem);
