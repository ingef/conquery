import * as React from "react";
import { useSelector, useDispatch } from "react-redux";
import T from "i18n-react";
import styled from "@emotion/styled";

import ReactSelect from "../../../form-components/ReactSelect";

import { setFormConfigsSearch } from "./actions";
import { StateT } from "app-types";

const Root = styled("div")`
  margin: 0 10px 5px;
  position: relative;
`;

const FormConfigsSearchBox: React.FC = () => {
  const search = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch
  );
  const options = useSelector<StateT, string[]>(
    (state) => state.formConfigs.names
  );

  const dispatch = useDispatch();

  const onSearch = (values: string[]) => dispatch(setFormConfigsSearch(values));

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

export default FormConfigsSearchBox;
