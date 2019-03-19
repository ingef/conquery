// @flow

import React from "react";
import T from "i18n-react";
import { components } from "react-select";
import { type FieldPropsType } from "redux-form";
import Dropzone from "react-dropzone";
import Markdown from "react-markdown";
import Mustache from "mustache";
import ReactTooltip from "react-tooltip";

import { type SelectOptionsType } from "../common/types/backend";
import { isEmpty } from "../common/helpers";
import InfoTooltip from "../tooltip/InfoTooltip";

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
  onDropFile?: Function,
  isOver: boolean,
  allowDropFile?: boolean
};

const InputMultiSelect = (props: PropsType) => {
  const allowDropFile = props.allowDropFile && !!props.onDropFile;

  const MultiValueLabel = params => {
    const label = params.data.optionLabel || params.data.label || params.data;
    const valueLabel = params.data.templateValues
      ? Mustache.render(label, params.data.templateValues)
      : label;

    return (
      <components.MultiValueLabel {...params}>
        <span data-tip={valueLabel}>{valueLabel}</span>
        <ReactTooltip type="info" place="top" effect="solid" />
      </components.MultiValueLabel>
    );
  };

  const options =
    props.options &&
    props.options.slice(0, 50).map(option => ({
      ...option,
      label:
        option.optionValue && option.templateValues
          ? Mustache.render(option.optionValue, option.templateValues)
          : option.label,
      value: "" + option.value, // convert number to string
      optionLabel: option.label
    }));

  return (
    <Labeled
      valueChanged={
        !isEmpty(props.input.value) &&
        props.input.value !== props.input.defaultValue
      }
      disabled={props.disabled}
      label={
        <>
          {props.label}
          {props.tooltip && <InfoTooltip text={props.tooltip} />}
        </>
      }
    >
      {props.input.value && props.input.value.length > 50 ? (
        <TooManyValues
          value={props.input.value}
          onClear={() => props.input.onChange(null)}
        />
      ) : (
        <Dropzone
          disableClick
          style={{ position: "relative", display: "block", maxWidth: "300px" }}
          activeClassName={allowDropFile ? "dropzone--over" : ""}
          className={allowDropFile ? "dropzone" : ""}
          onDrop={files => props.onDropFile(files[0])}
          disabled={!allowDropFile}
        >
          <ReactSelect
            creatable
            name="form-field"
            options={options}
            components={{ MultiValueLabel }}
            value={props.input.value}
            onChange={props.input.onChange}
            isDisabled={props.disabled}
            isMulti
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
            isLoading={!!props.isLoading}
            classNamePrefix={"react-select"}
            closeMenuOnSelect={false}
            formatOptionLabel={({
              label,
              optionValue,
              templateValues,
              highlight
            }) =>
              optionValue && templateValues ? (
                <Markdown
                  source={Mustache.render(optionValue, templateValues)}
                />
              ) : (
                label
              )
            }
            filterOption={false}
          />
        </Dropzone>
      )}
    </Labeled>
  );
};

export default InputMultiSelect;
