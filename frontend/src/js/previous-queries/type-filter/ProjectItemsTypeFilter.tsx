import { type ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";
import FormSymbol from "../../symbols/FormSymbol";
import QuerySymbol from "../../symbols/QuerySymbol";

import { setTypeFilter } from "./actions";
import type { ProjectItemsTypeFilterStateT } from "./reducer";

const ProjectItemsTypeFilter = ({ className }: { className?: string }) => {
  const { t } = useTranslation();
  const OPTIONS: {
    value: ProjectItemsTypeFilterStateT;
    label: () => ReactNode;
    tooltip?: string;
  }[] = useMemo(
    () => [
      {
        value: "all",
        label: () => t("projectItemsFilter.all"),
      },
      {
        value: "queries",
        label: () => <QuerySymbol />,
        tooltip: t("projectItemsTypeFilter.queries"),
      },
      {
        value: "configs",
        label: () => <FormSymbol />,
        tooltip: t("projectItemsTypeFilter.configs"),
      },
    ],
    [t],
  );

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
