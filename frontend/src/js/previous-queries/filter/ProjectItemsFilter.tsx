import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import { ToggleButton } from "../../ui-components/ToggleButton";
import { ToggleButtonGroup } from "../../ui-components/ToggleButtonGroup";

import { setFilter } from "./actions";
import type { ProjectItemsFilterStateT } from "./reducer";

const isFilter = (key: unknown): key is ProjectItemsFilterStateT =>
  key === "all" || key === "own" || key === "shared" || key === "system";

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

  const selectedFilter = useSelector<StateT, ProjectItemsFilterStateT>(
    (state) => state.projectItemsFilter,
  );
  const dispatch = useDispatch();

  return (
    <ToggleButtonGroup
      size="sm"
      aria-label={t("projectItemsFilter.label")}
      selectionMode="single"
      disallowEmptySelection
      selectedKeys={[selectedFilter]}
      onSelectionChange={(keys) => {
        const [key] = keys;
        if (isFilter(key)) dispatch(setFilter(key));
      }}
    >
      {OPTIONS.map(({ value, label }) => (
        <ToggleButton key={value} id={value}>
          {label}
        </ToggleButton>
      ))}
    </ToggleButtonGroup>
  );
};

export default ProjectItemsFilter;
