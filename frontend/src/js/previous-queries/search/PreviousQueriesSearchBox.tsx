import styled from "@emotion/styled";
import { StateT } from "app-types";
import * as React from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import ReactSelect from "../../form-components/ReactSelect";

import { updatePreviousQueriesSearch } from "./actions";

const Root = styled("div")`
  margin: 0 10px 5px;
  position: relative;
`;

const PreviousQueriesSearchBox: React.FC = () => {
  const { t } = useTranslation();
  const search = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch,
  );
  const options = useSelector<StateT, string[]>(
    (state) => state.previousQueries.names,
  );

  const dispatch = useDispatch();

  const onSearch = (values: string[]) =>
    dispatch(updatePreviousQueriesSearch(values));

  return (
    <Root>
      <ReactSelect
        creatable
        isMulti
        name="input"
        value={search.map((t) => ({ label: t, value: t }))}
        options={options ? options.map((t) => ({ label: t, value: t })) : []}
        onChange={(values) =>
          onSearch(values ? values.map((v) => v.value) : [])
        }
        placeholder={t("reactSelect.searchPlaceholder")}
        noOptionsMessage={() => t("reactSelect.noResults")}
        formatCreateLabel={(inputValue) =>
          t("common.create") + `: "${inputValue}"`
        }
      />
    </Root>
  );
};

export default PreviousQueriesSearchBox;
