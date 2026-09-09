import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import { Tab, TabList, Tabs } from "../../ui-components/Tabs";

import { setFilter } from "./actions";
import type { ProjectItemsFilterStateT } from "./reducer";

const ProjectItemsFilter = () => {
  const { t } = useTranslation();
  const OPTIONS: { value: ProjectItemsFilterStateT; label: string }[] = useMemo(
    () => [
      { value: "all", label: t("projectItemsFilter.all") },
      { value: "own", label: t("projectItemsFilter.own") },
      { value: "shared", label: t("projectItemsFilter.shared") },
      { value: "system", label: t("projectItemsFilter.system") },
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
    <Tabs
      size="sm"
      selectedKey={selectedFilter}
      onSelectionChange={(key) => onSetFilter(key as ProjectItemsFilterStateT)}
    >
      <TabList aria-label={t("projectItemsFilter.tabs")}>
        {OPTIONS.map(({ value, label }) => (
          <Tab key={value} id={value}>
            {label}
          </Tab>
        ))}
      </TabList>
    </Tabs>
  );
};

export default ProjectItemsFilter;
