import styled from "@emotion/styled";
import { faPlus, faTimes } from "@fortawesome/free-solid-svg-icons";
import type { ReactNode } from "react";

import IconButton from "../../button/IconButton";

interface PropsT {
  className?: string;
  label?: string;
  items: ReactNode[];
  limit: number;
  onAddClick: () => void;
  onRemoveClick: (idx: number) => void;
}

const Container = styled.div`
  display: flex;
  flex-wrap: wrap;
`;

const AddBtn = styled(IconButton)``;

const RemoveBtn = styled(IconButton)`
  position: absolute;
  top: -5px;
  right: -7px;
  background-color: white;
`;

const GroupItem = styled("div")`
  padding: 2px 2px 2px 0;
  position: relative;
  margin: 0 5px 5px 0;
`;

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
    <Container className={className}>
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
            <RemoveBtn tiny icon={faTimes} onClick={() => onRemoveClick(idx)} />
          )}
        </GroupItem>
      ))}
      {limitNotReached && <AddBtn icon={faPlus} tiny onClick={onAddClick} />}
    </Container>
  );
};

export default DynamicInputGroup;
