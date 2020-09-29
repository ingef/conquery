import * as React from "react";
import { useSelector, useDispatch } from "react-redux";
import T from "i18n-react";
import styled from "@emotion/styled";

import ReactSelect from "../../form-components/ReactSelect";

import { updatePreviousQueriesSearch } from "./actions";
import { StateT } from "app-types";

const Root = styled("div")`
  margin: 0 10px 5px;
  position: relative;
`;

const PreviousQueriesSearchBox: React.FC = () => {
  const search = useSelector<StateT, string[]>(
    (state) => state.previousQueriesSearch
  );
  const options = useSelector<StateT, string[]>(
    (state) => state.previousQueries.names
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
        placeholder={T.translate("reactSelect.searchPlaceholder")}
        noOptionsMessage={() => T.translate("reactSelect.noResults")}
        formatCreateLabel={(inputValue) =>
          T.translate("common.create") + `: "${inputValue}"`
        }
      />
    </Root>
  );
};

export default PreviousQueriesSearchBox;
