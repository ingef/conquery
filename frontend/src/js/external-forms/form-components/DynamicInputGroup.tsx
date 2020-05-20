import React from "react";
import styled from "@emotion/styled";

import IconButton from "../../button/IconButton";

type PropsType = {
  className?: string;
  label?: string;
  items: Array<Element>;
  limit?: number;
  onAddClick: Function;
  onRemoveClick: Function;
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

const DynamicInputGroup = ({
  className,
  label,
  items,
  limit,
  onRemoveClick,
  onAddClick
}: PropsType) => {
  // 0 means "infinite"
  const limitNotReached = limit === 0 || items.length < limit;

  return (
    <div className={className}>
      {label && <span>{label}</span>}
      {items.map((item, idx) => (
        <GroupItem key={idx}>
          {item}
          {/*
            No need to display the remove button, when limit is 1.
            Assumes that this component is always nested within another container 
            that allows to remove that item.

            In case you stumble accross this and you're not sure,
            you can also just delete the following constraint:
           */}
          {limit !== 1 && (
            <RemoveBtn tiny icon="times" onClick={() => onRemoveClick(idx)} />
          )}
        </GroupItem>
      ))}
      {limitNotReached && <AddBtn icon="plus" tiny onClick={onAddClick} />}
    </div>
  );
};

export default DynamicInputGroup;
