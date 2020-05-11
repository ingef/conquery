import React, { FC } from "react";
import T from "i18n-react";

import styled from "@emotion/styled";
import { useSelector, useDispatch } from "react-redux";
import { StateT } from "app-types";
import { setFormConfigsFilter } from "./actions";
import SmallTabNavigation from "js/small-tab-navigation/SmallTabNavigation";

const SxSmallTabNavigation = styled(SmallTabNavigation)`
  margin-bottom: 5px;
  padding: 0 10px;
`;

const FormConfigsFilter: FC = () => {
  const OPTIONS = [
    {
      value: "all",
      label: T.translate("previousQueriesFilter.all") as string,
    },
    {
      value: "activeForm",
      label: T.translate("formConfigsFilter.activeForm") as string,
    },
    {
      value: "own",
      label: T.translate("previousQueriesFilter.own") as string,
    },
    {
      value: "system",
      label: T.translate("previousQueriesFilter.system") as string,
    },
    {
      value: "shared",
      label: T.translate("previousQueriesFilter.shared") as string,
    },
  ];

  const selectedFilter = useSelector<StateT, string>(
    (state) => state.formConfigsFilter
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
