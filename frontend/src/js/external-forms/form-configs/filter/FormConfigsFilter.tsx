import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import SmallTabNavigation from "../../../small-tab-navigation/SmallTabNavigation";

import { setFormConfigsFilter } from "./actions";

const SxSmallTabNavigation = styled(SmallTabNavigation)`
  margin-bottom: 5px;
  padding: 0 10px;
`;

const FormConfigsFilter: FC = () => {
  const { t } = useTranslation();
  const OPTIONS = [
    {
      value: "own",
      label: t("previousQueriesFilter.own") as string,
    },
    {
      value: "all",
      label: t("previousQueriesFilter.all") as string,
    },
    {
      value: "activeForm",
      label: t("formConfigsFilter.activeForm") as string,
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
    (state) => state.formConfigsFilter,
  );
  const dispatch = useDispatch();
  const setFilter = (filter: string) => dispatch(setFormConfigsFilter(filter));

  return (
    <SxSmallTabNavigation
      className="form-configs-filter"
      options={OPTIONS}
      selectedTab={selectedFilter}
      onSelectTab={(tab) => setFilter(tab)}
    />
  );
};

export default FormConfigsFilter;
