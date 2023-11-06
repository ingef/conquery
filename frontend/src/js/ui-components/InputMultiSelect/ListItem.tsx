import { Ref, memo } from "react";

import type { SelectOptionT } from "../../api/types";
import { SxSelectListOption } from "../InputSelect/InputSelectComponents";

interface Props {
  item: SelectOptionT;
  index: number;
  highlightedIndex: number;
  getItemProps: (props: {
    index: number;
    item: SelectOptionT;
  }) => { ref?: Ref<HTMLDivElement> } & object;
}

const ListItem = ({ item, index, highlightedIndex, getItemProps }: Props) => {
  const { ref: itemPropsRef, ...itemProps } = getItemProps({
    index,
    item: item,
  });

  return (
    <SxSelectListOption
      active={highlightedIndex === index}
      option={item}
      {...itemProps}
      ref={itemPropsRef}
    />
  );
};

export default memo(ListItem);
