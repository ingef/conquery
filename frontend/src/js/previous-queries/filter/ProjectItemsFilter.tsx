import { StateT } from "app-types";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";

import { setFilter } from "./actions";
import { ProjectItemsFilterStateT } from "./reducer";

interface Props {
  className?: string;
}

const ProjectItemsFilter: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const OPTIONS: { value: ProjectItemsFilterStateT; label: string }[] = [
    {
      value: "all",
      label: t("projectItemsFilter.all") as string,
    },
    {
      value: "own",
      label: t("projectItemsFilter.own") as string,
    },
    {
      value: "shared",
      label: t("projectItemsFilter.shared") as string,
    },
    {
      value: "system",
      label: t("projectItemsFilter.system") as string,
    },
  ];

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
