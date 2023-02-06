import { FC, ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import type { StateT } from "../../app/reducers";
import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";

import { setFilter } from "./actions";
import { ProjectItemsFilterStateT } from "./reducer";

interface Props {
  className?: string;
}

const ProjectItemsFilter: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const OPTIONS: { value: ProjectItemsFilterStateT; label: () => ReactNode }[] =
    useMemo(
      () => [
        {
          value: "all",
          label: () => t("projectItemsFilter.all"),
        },
        {
          value: "own",
          label: () => t("projectItemsFilter.own"),
        },
        {
          value: "shared",
          label: () => t("projectItemsFilter.shared"),
        },
        {
          value: "system",
          label: () => t("projectItemsFilter.system"),
        },
      ],
      [t],
    );

  const selectedFilter = useSelector<StateT, string>(
    (state) => state.projectItemsFilter,
  );
  const dispatch = useDispatch();
  const onSetFilter = (filter: ProjectItemsFilterStateT) =>
    dispatch(setFilter(filter));

  return (
    <SmallTabNavigation
      className={className}
      options={OPTIONS}
      selectedTab={selectedFilter}
      onSelectTab={(tab) => onSetFilter(tab as ProjectItemsFilterStateT)}
    />
  );
};

export default ProjectItemsFilter;
