import styled from "@emotion/styled";
import { StateT } from "app-types";
import { FC } from "react";
import { useTranslation } from "react-i18next";
import { useSelector, useDispatch } from "react-redux";

import InputMultiSelect from "../../../ui-components/InputMultiSelect/InputMultiSelect";

import { setFormConfigsSearch } from "./actions";

const Root = styled("div")`
  margin: 0 10px 5px;
  position: relative;
`;

const FormConfigsSearchBox: FC = () => {
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
      <InputMultiSelect
        creatable
        value={search.map((t) => ({ label: t, value: t }))}
        options={options ? options.map((t) => ({ label: t, value: t })) : []}
        onChange={(values) =>
          onSearch(values ? values.map((v) => v.value as string) : [])
        }
        placeholder={t("inputSelect.searchPlaceholder")}
      />
    </Root>
  );
};

export default FormConfigsSearchBox;
