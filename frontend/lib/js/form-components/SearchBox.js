// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { isEmpty, duration } from "../common/helpers";
import ReactSelect from "../form-components/ReactSelect";
import IconButton from "../button/IconButton";
import AnimatedDots from "../common/components/AnimatedDots";
import ClearableInput from "../form-components/ClearableInput";

const Root = styled("div")`
  margin: 0 10px 5px 20px;
  position: relative;
`;

const StyledClearableInput = styled(ClearableInput)`
  width: 100%;
  input {
    width: 100%;
    &::placeholder {
      color: ${({ theme }) => theme.col.grayMediumLight};
      opacity: 1;
    }
  }
`;

const Right = styled("div")`
  position: absolute;
  top: 0px;
  right: 30px;
  display: flex;
  flex-direction: row;
  align-items: center;
  height: 36px;
`;

const StyledIconButton = styled(IconButton)`
  color: ${({ theme }) => theme.col.gray};
`;

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
  const {
    datasetId,
    searchResult,
    searchConfig,
    isMulti,
    search,
    options,
    onSearch,
    onChange,
    onClearQuery
  } = props;

  return (
    <Root>
      {isMulti ? (
        <ReactSelect
          creatable
          isMulti
          name="input"
          value={search.map(t => ({ label: t, value: t }))}
          options={options ? options.map(t => ({ label: t, value: t })) : []}
          onChange={values => onSearch(values.map(v => v.value))}
          placeholder={T.translate("reactSelect.searchPlaceholder")}
          noOptionsMessage={() => T.translate("reactSelect.noResults")}
        />
      ) : (
        <div>
          <StyledClearableInput
            placeholder={T.translate("search.placeholder")}
            value={searchResult.query || ""}
            onChange={value => {
              return isEmpty(value)
                ? onClearQuery()
                : onChange(value) || onSearch(value);
            }}
            inputProps={{
              onKeyPress: e => {
                return e.key === "Enter"
                  ? onSearch(
                      props.datasetId,
                      e.target.value,
                      searchConfig.limit
                    )
                  : null;
              }
            }}
          />
          {!isEmpty(searchResult.query) && (
            <Right>
              <StyledIconButton
                icon="search"
                aria-hidden="true"
                onClick={() =>
                  onSearch(datasetId, searchResult.query, searchConfig.limit)
                }
              />
            </Right>
          )}
          {searchResult.loading ? (
            <AnimatedDots />
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
        </div>
      )}
    </Root>
  );
};

export default SearchBox;
