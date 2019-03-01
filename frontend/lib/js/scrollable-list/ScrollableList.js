// @flow

import React from "react";
import classnames from "classnames";
import ReactList from "react-list";

type PropsType = {
  items: Array,
  maxVisibleItems?: number,
  fullWidth?: boolean,
  minWidth?: boolean
};

const ScrollableList = (props: PropsType) => {
  const renderItem = (index, key) => {
    return (
      <div key={key} className="scrollable-list-item">
        {props.items[index]}
      </div>
    );
  };

  const itemHeight = 34; // pixels, as defined in scrollableList.sass

  // If the number of visible items is specified here,
  // make an additional element half-visible at the end to indicate
  // that the list is scrollable
  const style = props.maxVisibleItems
    ? { maxHeight: `${(props.maxVisibleItems + 0.5) * itemHeight}px` }
    : {};

  return (
    <div
      className={classnames("scrollable-list", {
        "scrollable-list--full-width": !!props.fullWidth,
        "scrollable-list--min-width": !!props.minWidth
      })}
      style={style}
    >
      <ReactList
        itemRenderer={renderItem}
        length={props.items ? props.items.length : 0}
        type="uniform"
      />
    </div>
  );
};

export default ScrollableList;
