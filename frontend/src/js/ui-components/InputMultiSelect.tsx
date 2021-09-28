import styled from "@emotion/styled";
import { theme } from "app-theme";
import Mustache from "mustache";
import React, { FC } from "react";
import { useTranslation } from "react-i18next";
import Markdown from "react-markdown";
import { components, MenuListComponentProps } from "react-select";
import { FixedSizeList } from "react-window";

import type { FilterSuggestion, SelectOptionT } from "../api/types";
import TransparentButton from "../button/TransparentButton";
import { exists } from "../common/helpers/exists";
import InfoTooltip from "../tooltip/InfoTooltip";

import InputMultiSelectDropzone from "./InputMultiSelectDropzone";
import Labeled from "./Labeled";
import ReactSelect from "./ReactSelect";
import TooManyValues from "./TooManyValues";

const SxInputMultiSelectDropzone = styled(InputMultiSelectDropzone)`
  display: block;
`;

const SxLabeled = styled(Labeled)`
  .fullwidth {
    width: 100%;
  }
`;

const SxMarkdown = styled(Markdown)`
  p {
    margin: 0;
  }
`;

const Row = styled("div")`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 10px;
  border-bottom: 1px solid #ccc;
`;

const Option = styled("div")<{ isSelected?: boolean }>`
  padding: 4px 10px;
  display: flex;
  align-items: center;
  font-size: ${({ theme }) => theme.font.sm};
  color: ${({ isSelected }) => (isSelected ? "white" : theme.col.black)};
  font-weight: ${({ isSelected }) => (isSelected ? 400 : 300)};
  cursor: pointer;
  background-color: ${({ isSelected }) =>
    isSelected ? theme.col.blueGrayDark : "white"};
  &:hover {
    background-color: ${({ isSelected }) =>
      isSelected ? theme.col.blueGrayDark : theme.col.blueGrayVeryLight};
  }
`;

const InfoText = styled("p")`
  margin: 0;
  color: ${({ theme }) => theme.col.gray};
  font-size: ${({ theme }) => theme.font.xs};
  margin-right: 10px;
`;

// Arbitrary number that has been set in the backend as well
// TODO: Unlimited here + paginated backend vs
const OPTIONS_LIMIT = 500;

const MultiValueLabel = (params: any) => {
  const label = params.data.optionLabel || params.data.label || params.data;
  const valueLabel = params.data.templateValues
    ? Mustache.render(label, params.data.templateValues)
    : label;

  return (
    <components.MultiValueLabel {...params}>
      <span>{valueLabel}</span>
    </components.MultiValueLabel>
  );
};

const optionContainsStr = (str: string) => (option: SelectOptionT) => {
  return (
    option.value.toString().toLowerCase().includes(str) ||
    option.label.toLowerCase().includes(str)
  );
};

export interface MultiSelectInputProps {
  defaultValue?: string[];
  value: SelectOptionT[] | FilterSuggestion[];
  onChange: (value: SelectOptionT[] | FilterSuggestion[] | null) => void;
}

export interface InputMultiSelectProps {
  label?: string;
  indexPrefix?: number;
  options: SelectOptionT[];
  disabled?: boolean;
  tooltip?: string;
  onInputChange?: (value: string) => void;
  isLoading?: boolean;
  className?: string;
  allowDropFile?: boolean | null;
  closeMenuOnSelect?: boolean;
  onDropFile?: (file: File) => void;

  input: MultiSelectInputProps;
}

const isFilterSuggestion = (
  val: SelectOptionT | FilterSuggestion,
): val is FilterSuggestion => {
  return (
    exists((val as any).optionValue) && exists((val as any).templateValues!)
  );
};

const InputMultiSelect: FC<InputMultiSelectProps> = (props) => {
  const { t } = useTranslation();

  const allowDropFile = props.allowDropFile && !!props.onDropFile;

  const hasTooManyValues =
    props.input.value && props.input.value.length > OPTIONS_LIMIT;

  const options = props.options.slice(0, OPTIONS_LIMIT).map((option) => ({
    ...option,
    label: isFilterSuggestion(option)
      ? Mustache.render(option.optionValue, option.templateValues)
      : option.label,
    value: String(option.value),
    optionLabel: option.label,
  }));

  const MenuList = ({
    children,
    ...ownProps
  }: MenuListComponentProps<SelectOptionT, true>) => {
    return (
      <>
        <Row>
          <InfoText>
            {!!props.options ? props.options.length : 0}{" "}
            {t("inputMultiSelect.options")}
          </InfoText>
          <TransparentButton
            tiny
            disabled={!props.options || props.options.length === 0}
            onClick={() => {
              const visibleOptions = props.options.filter(
                optionContainsStr(ownProps.selectProps.inputValue),
              );

              ownProps.setValue(visibleOptions, "select-option");
            }}
          >
            {t("inputMultiSelect.insertAll")}
          </TransparentButton>
        </Row>
        <FixedSizeList
          height={300}
          itemSize={30}
          width="100%"
          itemCount={ownProps.options.length}
          {...ownProps}
        >
          {({ index, style }) => (
            <Option
              title={ownProps.options[index].label}
              onClick={() => ownProps.selectOption(ownProps.options[index])}
              isSelected={ownProps.getValue().includes(ownProps.options[index])}
              style={style}
            >
              {ownProps.options[index].label}
            </Option>
          )}
        </FixedSizeList>
      </>
    );
  };

  const Select = (
    <ReactSelect<true>
      creatable
      highlightChanged
      isSearchable
      isMulti
      isMenuOpen={true}
      className="fullwidth"
      createOptionPosition="first"
      name="form-field"
      options={options}
      components={{ MultiValueLabel, MenuList }}
      value={props.input.value}
      isDisabled={props.disabled}
      menuIsOpen
      isLoading={!!props.isLoading}
      classNamePrefix={"react-select"}
      maxMenuHeight={300}
      closeMenuOnSelect={!!props.closeMenuOnSelect}
      placeholder={
        allowDropFile
          ? t("inputMultiSelect.dndPlaceholder")
          : t("inputSelect.placeholder")
      }
      noOptionsMessage={() => t("inputSelect.empty")}
      onChange={props.input.onChange}
      onInputChange={
        props.onInputChange || // To allow for async option loading
        function (value: string) {
          return value;
        }
      }
      formatCreateLabel={(inputValue: string) =>
        t("common.create") + `: "${inputValue}"`
      }
      formatOptionLabel={({ label, optionValue, templateValues, highlight }) =>
        optionValue && templateValues ? (
          <SxMarkdown source={Mustache.render(optionValue, templateValues)} />
        ) : (
          label
        )
      }
    />
  );

  return (
    <SxLabeled
      className={props.className}
      valueChanged={
        exists(props.input.value) &&
        JSON.stringify(props.input.value) !==
          JSON.stringify(props.input.defaultValue)
      }
      disabled={!!props.disabled}
      label={
        <>
          {props.label}
          {props.tooltip && <InfoTooltip text={props.tooltip} />}
        </>
      }
      indexPrefix={props.indexPrefix}
    >
      {hasTooManyValues && (
        <TooManyValues
          count={props.input.value.length}
          onClear={() => props.input.onChange(null)}
        />
      )}
      {!hasTooManyValues && !allowDropFile && Select}
      {!hasTooManyValues && allowDropFile && props.onDropFile && (
        <SxInputMultiSelectDropzone onDropFile={props.onDropFile}>
          {() => Select}
        </SxInputMultiSelectDropzone>
      )}
    </SxLabeled>
  );
};

export default InputMultiSelect;
