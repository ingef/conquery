import React from "react";
import styled from "@emotion/styled";

import IconButton from "../button/IconButton";

type PropsType = {
  className?: string,
  label?: string,
  items: Array<Element>,
  onAddClick: Function,
  onRemoveClick: Function,
  canExpand: boolean
};

const AddBtn = styled(IconButton)`
  margin: 10px;
`;

const RemoveBtn = styled(IconButton)`
  position: absolute;
  top: 0;
  right: 0;
`;

const GroupItem = styled("div")`
  padding: 0 20px 0 0;
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
        <RemoveBtn icon="times" onClick={() => props.onRemoveClick(idx)} />
      </GroupItem>
    ))}
    <AddBtn icon="plus" onClick={props.onAddClick} />
  </div>
);

export default DynamicInputGroup;
