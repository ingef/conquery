import { StateT } from "app-types";
import { FC } from "react";
import { useDispatch, useSelector } from "react-redux";

import Tags from "../../tags/Tags";
import {
  addFolderToFilter,
  removeFolderFromFilter,
} from "../folderFilter/actions";

interface PropsT {
  className?: string;
  tags?: string[];
}

const getSelectedTags = (
  tags: string[],
  folderFilter: string[],
  search: string[],
) =>
  (tags || []).map((tag) => {
    const tagSelectedByFolderFilter = folderFilter.some(
      (folder) => tag === folder,
    );
    const tagSelectedBySearch = search.some((str) =>
      tag.toLowerCase().includes(str.toLowerCase()),
    );

    return {
      label: tag,
      isSelected: tagSelectedByFolderFilter || tagSelectedBySearch,
    };
  });

const PreviousQueryTags: FC<PropsT> = ({ className, tags }) => {
  const search = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch,
  );
  const folderFilter = useSelector<StateT, string[]>(
    (state) => state.previousQueriesFolderFilter.folders,
  );
  const selectedTags = getSelectedTags(tags || [], folderFilter, search);

  const dispatch = useDispatch();

  const onClickTag = (tag: string) => {
    if (!folderFilter.includes(tag)) {
      dispatch(addFolderToFilter(tag));
    } else {
      dispatch(removeFolderFromFilter(tag));
    }
  };

  return (
    <Tags className={className} tags={selectedTags} onClickTag={onClickTag} />
  );
};

export default PreviousQueryTags;
