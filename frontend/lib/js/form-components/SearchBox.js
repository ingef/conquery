// @flow

import React from "react";
import { Dot } from "react-animated-dots";
import T from "i18n-react";
import { Creatable as Select } from "react-select";
import { isEmpty, duration } from "../common/helpers";

type PropsType = {
  search: string[],
  onSearch: string => void,
  onChange: () => void,
  onClearQuery: () => void,
  options: string[],
  isMulti: boolean,
  searchResult: Object,
  datasetId: string,
  searchConfig: Object
};

const SearchBox = (props: PropsType) => {
  const { searchResult, searchConfig } = props;

  return props.isMulti ? (
    <div className="search-box">
      <Select
        name="input"
        value={props.search.map(t => ({ label: t, value: t }))}
        options={
          props.options ? props.options.map(t => ({ label: t, value: t })) : []
        }
        onChange={values => props.onSearch(values.map(v => v.value))}
        isMulti
        placeholder={T.translate("reactSelect.searchPlaceholder")}
        noOptionsMessage={() => T.translate("reactSelect.noResults")}
      />
    </div>
  ) : (
    <div className="search-box input--full-width">
      <input
        className="search-box__input"
        placeholder={T.translate("search.placeholder")}
        value={searchResult.query || ""}
        onChange={e => {
          return isEmpty(e.target.value)
            ? props.onClearQuery()
            : props.onChange(e.target.value) || props.onSearch(e.target.value);
        }}
        onKeyPress={e => {
          return e.key === "Enter"
            ? props.onSearch(
                props.datasetId,
                e.target.value,
                searchConfig.limit
              )
            : null;
        }}
      />
      {searchResult.loading ? (
        <span className="dots">
          <Dot>.</Dot>
          <Dot>.</Dot>
          <Dot>.</Dot>
        </span>
      ) : (
        searchResult.searching &&
        searchResult.resultCount >= 0 && (
          <span className="input input-label--disabled input-label--tiny">
            {T.translate("search.resultLabel", {
              limit: searchResult.limit,
              resultCount: searchResult.resultCount,
              duration: duration(
                searchResult.duration,
                "milliseconds",
                T.translate("search.durationFormat")
              )
            })}
          </span>
        )
      )}
      {!isEmpty(searchResult.query) && (
        <div>
          <i
            className="search-box__search-icon fa fa-search fa-1"
            aria-hidden="true"
            onClick={() =>
              props.onSearch(
                props.datasetId,
                searchResult.query,
                searchConfig.limit
              )
            }
          />
          <span
            className="search-box__clear-zone"
            title={T.translate("common.clearValue")}
            aria-label={T.translate("common.clearValue")}
            onClick={() => props.onClearQuery() || props.onSearch("")}
          >
            ×
          </span>
        </div>
      )}
    </div>
  );
};

export default SearchBox;
