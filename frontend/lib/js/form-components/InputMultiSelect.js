// @flow

import React from "react";
import styled from "@emotion/styled";
import { NativeTypes } from "react-dnd-html5-backend";
import T from "i18n-react";
import { components } from "react-select";
import { type FieldPropsType } from "redux-form";
import Markdown from "react-markdown";
import Mustache from "mustache";

import { type SelectOptionsType } from "../common/types/backend";
import { isEmpty } from "../common/helpers";
import InfoTooltip from "../tooltip/InfoTooltip";

import Dropzone from "./Dropzone";
import TooManyValues from "./TooManyValues";
import ReactSelect from "./ReactSelect";
import Labeled from "./Labeled";

type PropsType = FieldPropsType & {
  label?: string,
  options: SelectOptionsType,
  disabled?: ?boolean,
  tooltip?: string,
  onInputChange?: Function,
  isLoading?: boolean,
  className?: string,

  allowDropFile?: boolean,
  onDropFile?: Function
};

const StyledDropzone = styled(Dropzone)`
  position: relative;
  display: block;
  max-width: 300px;
`;

const OPTIONS_LIMIT = 50;

const InputMultiSelect = (props: PropsType) => {
  const allowDropFile = props.allowDropFile && !!props.onDropFile;

  const MultiValueLabel = params => {
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

  const hasTooManyValues =
    props.input.value && props.input.value.length > OPTIONS_LIMIT;

  const options =
    props.options &&
    props.options.slice(0, OPTIONS_LIMIT).map(option => ({
      ...option,
      label:
        option.optionValue && option.templateValues
          ? Mustache.render(option.optionValue, option.templateValues)
          : option.label,
      value: "" + option.value, // convert number to string
      optionLabel: option.label
    }));

  const Select = (
    <ReactSelect
      creatable
      isMulti
      createOptionPosition="first"
      name="form-field"
      options={options}
      components={{ MultiValueLabel }}
      value={props.input.value}
      onChange={props.input.onChange}
      isDisabled={props.disabled}
      isLoading={!!props.isLoading}
      filterOption={false}
      classNamePrefix={"react-select"}
      closeMenuOnSelect={false}
      placeholder={
        allowDropFile
          ? T.translate("reactSelect.dndPlaceholder")
          : T.translate("reactSelect.placeholder")
      }
      noOptionsMessage={() => T.translate("reactSelect.noResults")}
      onInputChange={
        props.onInputChange ||
        function(value) {
          return value;
        }
      }
      formatOptionLabel={({ label, optionValue, templateValues, highlight }) =>
        optionValue && templateValues ? (
          <Markdown source={Mustache.render(optionValue, templateValues)} />
        ) : (
          label
        )
      }
    />
  );

  return (
    <Labeled
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
      {!hasTooManyValues && allowDropFile && (
        <StyledDropzone
          acceptedDropTypes={NativeTypes.FILE}
          onDrop={(x, monitor) => {
            const item = monitor.getItem();

            if (item && props.onDropFile) {
              props.onDropFile(item.files[0]);
            }
          }}
        >
          {Select}
        </StyledDropzone>
      )}
      {!hasTooManyValues && !allowDropFile && Select}
    </Labeled>
  );
};

export default InputMultiSelect;
