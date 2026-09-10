import { type ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../../app/reducers";
import FormSymbol from "../../symbols/FormSymbol";
import QuerySymbol from "../../symbols/QuerySymbol";
import { ToggleButton } from "../../ui-components/ToggleButton";
import { ToggleButtonGroup } from "../../ui-components/ToggleButtonGroup";
import { Tooltip, TooltipTrigger } from "../../ui-components/Tooltip";

import { setTypeFilter } from "./actions";
import type { ProjectItemsTypeFilterStateT } from "./reducer";

const isTypeFilter = (key: unknown): key is ProjectItemsTypeFilterStateT =>
  key === "all" || key === "queries" || key === "configs";

const ProjectItemsTypeFilter = () => {
  const { t } = useTranslation();
  const OPTIONS: {
    value: ProjectItemsTypeFilterStateT;
    label: ReactNode;
    tooltip?: string;
  }[] = useMemo(
    () => [
      { value: "all", label: t("projectItemsFilter.all") },
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
    ],
    [t],
  );

  const selectedFilter = useSelector<StateT, ProjectItemsTypeFilterStateT>(
    (state) => state.projectItemsTypeFilter,
  );
  const dispatch = useDispatch();

  return (
    <ToggleButtonGroup
      segmented
      size="sm"
      aria-label={t("projectItemsTypeFilter.label")}
      selectionMode="single"
      disallowEmptySelection
      selectedKeys={[selectedFilter]}
      onSelectionChange={(keys) => {
        const [key] = keys;
        if (isTypeFilter(key)) dispatch(setTypeFilter(key));
      }}
    >
      {OPTIONS.map(({ value, label, tooltip }) => (
        <TooltipTrigger key={value}>
          <ToggleButton id={value} aria-label={tooltip}>
            {label}
          </ToggleButton>
          <Tooltip>{tooltip}</Tooltip>
        </TooltipTrigger>
      ))}
    </ToggleButtonGroup>
  );
};

export default ProjectItemsTypeFilter;
