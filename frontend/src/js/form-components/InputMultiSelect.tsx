import React, { FC } from "react";
import styled from "@emotion/styled";
import { useTranslation } from "react-i18next";
import { components } from "react-select";
import Markdown from "react-markdown";
import Mustache from "mustache";

import type { FilterSuggestion, SelectOptionT } from "../api/types";
import { exists } from "../common/helpers/exists";
import TransparentButton from "../button/TransparentButton";
import InfoTooltip from "../tooltip/InfoTooltip";

import InputMultiSelectDropzone from "./InputMultiSelectDropzone";
import TooManyValues from "./TooManyValues";
import ReactSelect from "./ReactSelect";
import Labeled from "./Labeled";

const SxInputMultiSelectDropzone = styled(InputMultiSelectDropzone)`
  display: block;
`;

const SxReactSelect = styled(ReactSelect)`
  width: 100%;
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

const InfoText = styled("p")`
  margin: 0;
  color: ${({ theme }) => theme.col.gray};
  font-size: ${({ theme }) => theme.font.xs};
  margin-right: 10px;
`;

// Arbitrary number that has been set in the backend as well
// TODO: Unlimited here + paginated backend vs
const OPTIONS_LIMIT = 50;

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
  onChange: (value: string[] | null) => void;
}

export interface InputMultiSelectProps {
  label?: string;
  options: SelectOptionT[];
  disabled?: boolean | null;
  tooltip?: string;
  onInputChange?: (value: string[] | null) => void;
  isLoading?: boolean;
  className?: string;
  allowDropFile?: boolean | null;
  closeMenuOnSelect?: boolean;
  onDropFile?: Function;

  input: MultiSelectInputProps;
}

// Typescript typeguard
const isFilterSuggestion = (
  val: SelectOptionT | FilterSuggestion
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

  const MenuList: FC = ({ children, ...ownProps }) => {
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
                optionContainsStr(ownProps.selectProps.inputValue)
              );

              ownProps.setValue(visibleOptions);
            }}
          >
            {t("inputMultiSelect.insertAll")}
          </TransparentButton>
        </Row>
        <components.MenuList {...ownProps}>{children}</components.MenuList>
      </>
    );
  };

  const Select = (
    <SxReactSelect
      creatable
      highlightChanged
      isSearchable
      isMulti
      createOptionPosition="first"
      name="form-field"
      options={options}
      components={{ MultiValueLabel, MenuList }}
      value={props.input.value}
      isDisabled={props.disabled}
      isLoading={!!props.isLoading}
      classNamePrefix={"react-select"}
      closeMenuOnSelect={!!props.closeMenuOnSelect}
      placeholder={
        allowDropFile
          ? t("reactSelect.dndPlaceholder")
          : t("reactSelect.placeholder")
      }
      noOptionsMessage={() => t("reactSelect.noResults")}
      onChange={props.input.onChange}
      onInputChange={
        props.onInputChange || // To allow for async option loading
        function (value: string[]) {
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
    <Labeled
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
    >
      {hasTooManyValues && (
        <TooManyValues
          value={props.input.value}
          onClear={() => props.input.onChange(null)}
        />
      )}
      {!hasTooManyValues && !allowDropFile && Select}
      {!hasTooManyValues && allowDropFile && (
        <SxInputMultiSelectDropzone onDropFile={props.onDropFile}>
          {() => Select}
        </SxInputMultiSelectDropzone>
      )}
    </Labeled>
  );
};

export default InputMultiSelect;
