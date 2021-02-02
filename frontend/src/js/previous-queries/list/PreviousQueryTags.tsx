import { StateT } from "app-types";
import React, { FC } from "react";
import { useDispatch, useSelector } from "react-redux";

import Tags from "../../tags/Tags";

import { addTagToPreviousQueriesSearch } from "../search/actions";
import type { PreviousQueriesSearchStateT } from "../search/reducer";

interface PropsT {
  tags?: string[];
}

const tagContainsAnySearch = (
  tag: string,
  searches: PreviousQueriesSearchStateT
) => {
  return searches.some(
    (search) => tag.toLowerCase().indexOf(search.toLowerCase()) !== -1
  );
};

const selectPreviousQueryTags = (state: StateT, tags?: string[]) =>
  (tags || []).map((tag) => ({
    label: tag,
    isSelected: tagContainsAnySearch(tag, state.previousQueriesSearch),
  }));

const PreviousQueryTags: FC<PropsT> = ({ tags }) => {
  const selectedTags = useSelector<
    StateT,
    { label: string; isSelected: boolean }[]
  >((state) => selectPreviousQueryTags(state, tags));

  const dispatch = useDispatch();

  const onClickTag = (tag: string) =>
    dispatch(addTagToPreviousQueriesSearch(tag));

  return <Tags tags={selectedTags} onClickTag={onClickTag} />;
};

export default PreviousQueryTags;
