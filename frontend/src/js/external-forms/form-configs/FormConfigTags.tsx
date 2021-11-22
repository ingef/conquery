import { StateT } from "app-types";
import { FC } from "react";
import { useSelector, useDispatch } from "react-redux";

import Tags from "../../tags/Tags";

import { addTagToFormConfigsSearch } from "./search/actions";

const tagContainsAnySearch = (tag: string, search: string[]) => {
  return search.some(
    (str) => tag.toLowerCase().indexOf(str.toLowerCase()) !== -1,
  );
};

interface PropsT {
  tags: string[];
}

const FormConfigTags: FC<PropsT> = ({ tags }) => {
  const search = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch,
  );

  const dispatch = useDispatch();
  const onClickTag = (tag: string) => dispatch(addTagToFormConfigsSearch(tag));

  const augmentedTags = tags.map((tag) => ({
    label: tag,
    isSelected: tagContainsAnySearch(tag, search),
  }));

  return <Tags tags={augmentedTags} onClickTag={onClickTag} />;
};

export default FormConfigTags;
