import { FC } from "react";

import Tag from "./Tag";

interface PropsT {
  className?: string;
  tags: {
    label: string;
    isSelected: boolean;
  }[];
  onClickTag: (label: string) => void;
}

const Tags: FC<PropsT> = ({ className, tags, onClickTag }) => {
  return (
    <div className={className}>
      {!tags || tags.length <= 0
        ? null
        : tags.map((tag, i) => (
            <Tag
              key={i}
              isSelected={tag.isSelected}
              onClick={() => onClickTag(tag.label)}
            >
              {tag.label}
            </Tag>
          ))}
    </div>
  );
};

export default Tags;
