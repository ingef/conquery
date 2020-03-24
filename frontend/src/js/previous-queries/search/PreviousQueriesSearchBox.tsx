import * as React from "react";
import { connect } from "react-redux";
import T from "i18n-react";
import styled from "@emotion/styled";

import ReactSelect from "../../form-components/ReactSelect";

import { updatePreviousQueriesSearch } from "./actions";

const Root = styled("div")`
  margin: 0 10px 5px;
  position: relative;
`;

const mapStateToProps = state => ({
  search: state.previousQueriesSearch,
  options: state.previousQueries.names
});

const mapDispatchToProps = dispatch => ({
  onSearch: values => dispatch(updatePreviousQueriesSearch(values))
});

type PropsT = {
  options: string[];
  search: string[];
  onSearch: (values: string[]) => void;
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(({ search, options, onSearch }: PropsT) => {
  return (
    <Root>
      <ReactSelect
        creatable
        isMulti
        name="input"
        value={search.map(t => ({ label: t, value: t }))}
        options={options ? options.map(t => ({ label: t, value: t })) : []}
        onChange={values => onSearch(values ? values.map(v => v.value) : [])}
        placeholder={T.translate("reactSelect.searchPlaceholder")}
        noOptionsMessage={() => T.translate("reactSelect.noResults")}
        formatCreateLabel={inputValue =>
          T.translate("common.create") + `: "${inputValue}"`
        }
      />
    </Root>
  );
});
