// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select, {
  components,
  createFilter
}                                 from 'react-select';
import { type FieldPropsType }    from 'redux-form';
import Dropzone                   from 'react-dropzone'
import Markdown                   from 'react-markdown';
import Mustache                   from 'mustache';
import classnames                 from 'classnames';
import ReactTooltip               from 'react-tooltip';

import { type SelectOptionsType } from '../common/types/backend';
import { isEmpty }                from '../common/helpers';
import InfoTooltip                from '../tooltip/InfoTooltip';

type PropsType = FieldPropsType & {
  label: string,
  options: SelectOptionsType,
  disabled?: ?boolean,
  tooltip?: string,
  onInputChange?: Function,
  isLoading?: boolean,
  className?: string,
  onDropFiles?: Function,
  isOver: boolean,
  allowDropFile?: ?boolean,
};

const InputMultiSelect = (props: PropsType) => {
  const allowDropFile = props.allowDropFile && !!props.onDropFiles

  const MultiValueLabel = (params) => {
    const label = params.data.optionLabel || params.data.label
    const valueLabel = params.data.templateValues
      ? Mustache.render(label, params.data.templateValues)
      : label
    return (
      <components.MultiValueLabel {...params}>
        <span data-tip={valueLabel}>{valueLabel}</span>
        <ReactTooltip type="info" place="top" effect="solid" />
      </components.MultiValueLabel>
    )
  };

  const options = props.options && props.options.map(option => ({
    ...option,
    label: option.optionValue && option.templateValues
      ? Mustache.render(option.optionValue, option.templateValues)
      : option.label,
    value: '' + option.value, // convert number to string
    optionLabel: option.label
  }))

  const filterOption = createFilter({
    ignoreCase: true,
    ignoreAccents: true,
    trim: true,
    matchFrom: 'any',
  });

  return (
    <label className={classnames(
      'input', {
        'input--value-changed':
          !isEmpty(props.input.value) && props.input.value !== props.input.defaultValue
      }
    )}>
      <p className={classnames(
        'input-label', {
          'input-label--disabled': !!props.disabled
        }
      )}>
        { props.label }
        { props.tooltip && <InfoTooltip text={props.tooltip} /> }
      </p>
      <Dropzone
        disableClick
        style={{position: "relative", display: "block", maxWidth: "300px"}}
        activeClassName={allowDropFile ? 'dropzone--over' : ''}
        className={allowDropFile ? 'dropzone' : ''}
        onDrop={props.onDropFiles}
        disabled={!allowDropFile}
      >
        <Select
          name="form-field"
          options={options}
          components={{ MultiValueLabel }}
          value={(props.input.value || []).map(id => options.find(option => option.value === id))}
          onChange={(value) => props.input.onChange((value || []).map(v => v.value))}
          isDisabled={props.disabled}
          isSearchable
          isMulti
          placeholder={allowDropFile
            ? T.translate('reactSelect.dndPlaceholder')
            : T.translate('reactSelect.placeholder')
          }
          noOptionsMessage={() => T.translate('reactSelect.noResults')}
          onInputChange={props.onInputChange || function(value) { return value; }}
          isLoading={props.isLoading}
          classNamePrefix={'react-select'}
          closeMenuOnSelect={false}
          formatOptionLabel={({ label, optionValue, templateValues, highlight }) =>
            optionValue && templateValues
              ? <Markdown source={Mustache.render(optionValue, templateValues)} />
              : label
          }
          filterOption={filterOption}
        />
      </Dropzone>
    </label>
  )
};

export default InputMultiSelect;
