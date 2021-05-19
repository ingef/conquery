import { StateT } from "app-types";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import SmallTabNavigation from "../../small-tab-navigation/SmallTabNavigation";

import { setPreviousQueriesFilter } from "./actions";

interface Props {
  className?: string;
}

const PreviousQueriesFilter: FC<Props> = ({ className }) => {
  const { t } = useTranslation();
  const OPTIONS = [
    {
      value: "all",
      label: t("previousQueriesFilter.all") as string,
    },
    {
      value: "own",
      label: t("previousQueriesFilter.own") as string,
    },
    {
      value: "system",
      label: t("previousQueriesFilter.system") as string,
    },
    {
      value: "shared",
      label: t("previousQueriesFilter.shared") as string,
    },
  ];

  const selectedFilter = useSelector<StateT, string>(
    (state) => state.previousQueriesFilter,
  );
  const dispatch = useDispatch();
  const setFilter = (filter: string) =>
    dispatch(setPreviousQueriesFilter(filter));

  return (
    <SmallTabNavigation
      className={className}
      options={OPTIONS}
      selectedTab={selectedFilter}
      onSelectTab={(tab) => setFilter(tab)}
    />
  );
};

export default PreviousQueriesFilter;
