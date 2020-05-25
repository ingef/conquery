import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import { components } from "react-select";
import type { FieldPropsType } from "redux-form";
import Markdown from "react-markdown";
import Mustache from "mustache";

import type { SelectOptionsT } from "../api/types";
import { isEmpty } from "../common/helpers";
import TransparentButton from "../button/TransparentButton";
import InfoTooltip from "../tooltip/InfoTooltip";

import InputMultiSelectDropzone from "./InputMultiSelectDropzone";
import TooManyValues from "./TooManyValues";
import ReactSelect from "./ReactSelect";
import Labeled from "./Labeled";

interface PropsType extends FieldPropsType {
  label?: string;
  options: SelectOptionsT | null;
  disabled?: boolean | null;
  tooltip?: string;
  onInputChange?: Function;
  isLoading?: boolean;
  className?: string;

  allowDropFile?: boolean | null;
  onDropFile?: Function;
}

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

const MultiValueLabel = (params) => {
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

const optionContainsStr = (str) => (option) => {
  return (
    option.value.toString().toLowerCase().includes(str) ||
    option.label.toLowerCase().includes(str)
  );
};

const InputMultiSelect = (props: PropsType) => {
  const allowDropFile = props.allowDropFile && !!props.onDropFile;

  const hasTooManyValues =
    props.input.value && props.input.value.length > OPTIONS_LIMIT;

  const options =
    !!props.options &&
    props.options.slice(0, OPTIONS_LIMIT).map((option) => ({
      ...option,
      label:
        !!option.optionValue && !!option.templateValues
          ? Mustache.render(option.optionValue, option.templateValues)
          : option.label,
      value: option.value.toString(),
      optionLabel: option.label,
    }));

  const MenuList = ({ children, ...ownProps }) => {
    return (
      <div>
        <Row>
          <InfoText>
            {!!props.options ? props.options.length : 0}{" "}
            {T.translate("inputMultiSelect.options")}
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
            {T.translate("inputMultiSelect.insertAll")}
          </TransparentButton>
        </Row>
        <components.MenuList {...ownProps}>{children}</components.MenuList>
      </div>
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
      closeMenuOnSelect={false}
      placeholder={
        allowDropFile
          ? T.translate("reactSelect.dndPlaceholder")
          : T.translate("reactSelect.placeholder")
      }
      noOptionsMessage={() => T.translate("reactSelect.noResults")}
      onChange={props.input.onChange}
      onInputChange={
        props.onInputChange || // To allow for async option loading
        function (value) {
          return value;
        }
      }
      formatCreateLabel={(inputValue) =>
        T.translate("common.create") + `: "${inputValue}"`
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
        !isEmpty(props.input.value) &&
        props.input.value !== props.input.defaultValue
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
