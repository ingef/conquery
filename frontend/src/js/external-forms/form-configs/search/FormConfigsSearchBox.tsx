import styled from "@emotion/styled";
import { StateT } from "app-types";
import * as React from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import ReactSelect from "../../../ui-components/ReactSelect";

import { setFormConfigsSearch } from "./actions";

const Root = styled("div")`
  margin: 0 10px 5px;
  position: relative;
`;

const FormConfigsSearchBox: React.FC = () => {
  const { t } = useTranslation();
  const search = useSelector<StateT, string[]>(
    (state) => state.formConfigsSearch,
  );
  const options = useSelector<StateT, string[]>(
    (state) => state.formConfigs.names,
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
        placeholder={t("inputSelect.searchPlaceholder")}
        noOptionsMessage={() => t("inputSelect.empty")}
        formatCreateLabel={(inputValue) =>
          t("common.create") + `: "${inputValue}"`
        }
      />
    </Root>
  );
};

export default FormConfigsSearchBox;
