import { memo, useMemo } from "react";

import { SelectOptionT } from "../../api/types";
import { SxSelectListOption } from "../InputSelect/InputSelectComponents";

interface Props {
  item: SelectOptionT;
  index: number;
  highlightedIndex: number;
  getItemProps: (props: { index: number; item: SelectOptionT }) => any;
}

const ListItem = ({ item, index, highlightedIndex, getItemProps }: Props) => {
  const { ref: itemPropsRef, ...itemProps } = useMemo(
    () =>
      getItemProps({
        index,
        item: item,
      }),
    [getItemProps, index, item],
  );

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
