// @flow

import * as React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";

import { isEmpty } from "../common/helpers";
import ReactSelect from "../form-components/ReactSelect";
import IconButton from "../button/IconButton";
import AnimatedDots from "../common/components/AnimatedDots";
import BaseInput from "../form-components/BaseInput";

const Root = styled("div")`
  margin: 0 10px 5px 20px;
  position: relative;
`;

const StyledBaseInput = styled(BaseInput)`
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

const TinyText = styled("p")`
  margin: 3px 0;
  font-size: ${({ theme }) => theme.font.xs};
  color: ${({ theme }) => theme.col.gray};
`;

const Row = styled("div")`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
`;

type PropsType = {
  // TODO: Disentangle Concept search from PreviousQuery search
  search: string[] | Object,
  onSearch: string => void,
  onChange: () => void,
  onClearQuery: () => void,
  options: string[],
  textAppend?: React.Node,
  placeholder?: string,
  isMulti: boolean,
  datasetId: string
};

const SearchBox = (props: PropsType) => {
  const {
    datasetId,
    search,
    isMulti,
    options,
    placeholder,
    textAppend,
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
          placeholder={
            placeholder || T.translate("reactSelect.searchPlaceholder")
          }
          noOptionsMessage={() => T.translate("reactSelect.noResults")}
        />
      ) : (
        <div>
          <StyledBaseInput
            placeholder={placeholder || T.translate("search.placeholder")}
            value={search.query || ""}
            onChange={value => {
              return isEmpty(value)
                ? onClearQuery()
                : onChange(value) || onSearch(value);
            }}
            inputProps={{
              onKeyPress: e => {
                return e.key === "Enter" && !isEmpty(e.target.value)
                  ? onSearch(props.datasetId, e.target.value)
                  : null;
              }
            }}
          />
          {!isEmpty(search.query) && (
            <Right>
              <StyledIconButton
                icon="search"
                aria-hidden="true"
                onClick={() => onSearch(datasetId, search.query)}
              />
            </Right>
          )}
          {search.loading ? (
            <AnimatedDots />
          ) : (
            search.result &&
            search.resultCount >= 0 && (
              <Row>
                <TinyText>
                  {T.translate("search.resultLabel", {
                    totalResults: search.resultCount,
                    duration: (search.duration / 1000.0).toFixed(2)
                  })}
                </TinyText>
                {textAppend}
              </Row>
            )
          )}
        </div>
      )}
    </Root>
  );
};

export default SearchBox;
