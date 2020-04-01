import React from "react";
import Tags from "../../tags/Tags";

import { addTagToPreviousQueriesSearch } from "../search/actions";
import { useSelector, useDispatch } from "react-redux";
import { StateT } from "app-types";

const tagContainsAnySearch = (tag: string, search: string[]) => {
  return search.some(
    str => tag.toLowerCase().indexOf(str.toLowerCase()) !== -1
  );
};

interface PropsT {
  tags: string[];
}

const FormConfigTags: React.FC<PropsT> = ({ tags }) => {
  // const search = useSelector<StateT, string>(state => state.formConfigsSearch);
  const dispatch = useDispatch();
  // const onClickTag = (tag: string) => dispatch(addTagToPreviousQueriesSearch(tag))
  const augmentedTags = tags.map(tag => ({
    label: tag,
    isSelected: false // tagContainsAnySearch(tag, search)
  }));

  return <Tags tags={augmentedTags} onClickTag={() => null} />;
};

export default FormConfigTags;
