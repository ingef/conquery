// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select, { components }     from 'react-select';
import makeAnimated               from 'react-select/lib/animated';
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

  const MultiValueLabel = (params) =>
      <components.MultiValueLabel {...params}>
        { markdown(params.data.label, params.data.template, params.children) }
      </components.MultiValueLabel>;

  const options = props.options && props.options.map(o => ({
    label: o.optionValue && o.template
      ? Mustache.render(o.label, o.template)
      : o.label,
    value: o.value,
    template: o.template,
    optionValue: o.optionValue
  }))

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
          formatOptionLabel={({ label, optionValue, template }) =>
            <Markdown source={
              optionValue && template
              ? Mustache.render(optionValue, template)
              : label}
            />
          }
        />
      </Dropzone>
    </label>
  )
};

export default InputMultiSelect;
