// @flow

import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { isEmpty, duration } from "../common/helpers";
import ReactSelect from "../form-components/ReactSelect";
import IconButton from "../button/IconButton";
import AnimatedDots from "../common/components/AnimatedDots";

const Root = styled("div")`
  margin-bottom: 5px;
  padding: 0 10px 0 20px;
`;

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
`;

const Input = styled("input")`
  width: 100%;
  &::placeholder {
    color: ${({ theme }) => theme.col.grayMediumLight};
    opacity: 1;
  }
`;

const ClearZone = styled("span")`
  position: absolute;
  top: 52px;
  right: 22px;
  cursor: pointer;
  color: ${({ theme }) => theme.col.gray};
`;
const StyledIconButton = styled(IconButton)`
  position: absolute;
  top: 55px;
  right: 45px;
  cursor: pointer;
  color: #999;
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
        <Row>
          <Input
            placeholder={T.translate("search.placeholder")}
            value={searchResult.query || ""}
            onChange={e => {
              return isEmpty(e.target.value)
                ? onClearQuery()
                : onChange(e.target.value) || onSearch(e.target.value);
            }}
            onKeyPress={e => {
              return e.key === "Enter"
                ? onSearch(props.datasetId, e.target.value, searchConfig.limit)
                : null;
            }}
          />
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
          {!isEmpty(searchResult.query) && (
            <div>
              <StyledIconButton
                icon="search"
                aria-hidden="true"
                onClick={() =>
                  onSearch(datasetId, searchResult.query, searchConfig.limit)
                }
              />
              <ClearZone
                title={T.translate("common.clearValue")}
                aria-label={T.translate("common.clearValue")}
                onClick={() => onClearQuery() || onSearch("")}
              >
                ×
              </ClearZone>
            </div>
          )}
        </Row>
      )}
    </Root>
  );
};

export default SearchBox;
