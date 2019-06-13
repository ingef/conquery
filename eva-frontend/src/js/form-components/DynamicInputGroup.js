import React from "react";
import styled from "@emotion/styled";

import IconButton from "conquery/lib/js/button/IconButton";

type PropsType = {
  className?: string,
  label?: string,
  items: Array<Element>,
  onAddClick: Function,
  onRemoveClick: Function
};

const AddBtn = styled(IconButton)``;

const RemoveBtn = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
`;

const GroupItem = styled("div")`
  padding: 2px 20px 2px 0;
  position: relative;
  display: inline-block;
  vertical-align: middle;
`;

const DynamicInputGroup = (props: PropsType) => (
  <div className={props.className}>
    {props.label && <span>{props.label}</span>}
    {props.items.map((item, idx) => (
      <GroupItem key={idx}>
        {item}
        <RemoveBtn tiny icon="times" onClick={() => props.onRemoveClick(idx)} />
      </GroupItem>
    ))}
    <AddBtn icon="plus" tiny onClick={props.onAddClick} />
  </div>
);

export default DynamicInputGroup;
