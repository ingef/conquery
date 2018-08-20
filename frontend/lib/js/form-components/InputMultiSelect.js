// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import {
  components,
  createFilter,
  Creatable as Select
}                                 from 'react-select';
import { type FieldPropsType }    from 'redux-form';
import Dropzone                   from 'react-dropzone'
import Markdown                   from 'react-markdown';
import Mustache                   from 'mustache'
import classnames                 from 'classnames';

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
  const markdown = (label, template, defaultValue) => (
    typeof label === 'string' && template
      ? <Markdown source={Mustache.render(label, template)} />
      : defaultValue
  )

  const MultiValueLabel = (params) => {
    const valueLabel = params.data.optionLabel
      ? markdown(params.data.optionLabel, params.data.template, params.children)
      : params.data.label
      return (
    <components.MultiValueLabel {...params}>
      <span data-tip={valueLabel}>{valueLabel}</span>
      <InfoTooltip text={valueLabel} symbol={false} place={'top'} />
    </components.MultiValueLabel>)
  };

  const options = props.options && props.options.map(o => ({
    label: o.optionValue && o.template
      ? Mustache.render(o.optionValue, o.template)
      : o.label,
    value: '' + o.value, // convert number to string
    template: o.template,
    optionValue: o.optionValue,
    optionLabel: o.label,
    highlight: props.input.value
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
          value={props.input.value}
          onChange={(value) => props.input.onChange(value)}
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
          formatOptionLabel={({ label, optionValue, template, highlight }) =>
            optionValue && template
              ? <Markdown source={Mustache.render(optionValue, template)} />
              : label
          }
          filterOption={filterOption}
        />
      </Dropzone>
    </label>
  )
};

export default InputMultiSelect;
