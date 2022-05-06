import { FC, ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import type { StateT } from "../../app/reducers";
import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";
import FormSymbol from "../../symbols/FormSymbol";
import QuerySymbol from "../../symbols/QuerySymbol";

import { setTypeFilter } from "./actions";
import { ProjectItemsTypeFilterStateT } from "./reducer";

interface Props {
  className?: string;
}

const ProjectItemsTypeFilter: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const OPTIONS: {
    value: ProjectItemsTypeFilterStateT;
    label: ReactNode;
    tooltip?: string;
  }[] = [
    {
      value: "all",
      label: t("projectItemsFilter.all"),
    },
    {
      value: "queries",
      label: <QuerySymbol />,
      tooltip: t("projectItemsTypeFilter.queries"),
    },
    {
      value: "configs",
      label: <FormSymbol />,
      tooltip: t("projectItemsTypeFilter.configs"),
    },
  ];

  const selectedFilter = useSelector<StateT, string>(
    (state) => state.projectItemsTypeFilter,
  );
  const dispatch = useDispatch();
  const onSetTypeFilter = (filter: ProjectItemsTypeFilterStateT) =>
    dispatch(setTypeFilter(filter));

  return (
    <SmallTabNavigation
      className={className}
      options={OPTIONS}
      selectedTab={selectedFilter}
      onSelectTab={(tab) =>
        onSetTypeFilter(tab as ProjectItemsTypeFilterStateT)
      }
    />
  );
};

export default ProjectItemsTypeFilter;
