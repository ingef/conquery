import React from "react";
import Tag from "./Tag";

interface PropsT {
  className?: string;
  tags: {
    label: string;
    isSelected: boolean;
  }[];
  onClickTag: (label: string) => void;
}

const Tags: React.FC<PropsT> = props => {
  if (!props.tags || props.tags.length <= 0) {
    return null;
  }

  return (
    <div className={props.className}>
      {props.tags.map((tag, i) => (
        <Tag
          key={i}
          isSelected={tag.isSelected}
          onClick={() => props.onClickTag(tag.label)}
        >
          {tag.label}
        </Tag>
      ))}
    </div>
  );
};

export default Tags;
