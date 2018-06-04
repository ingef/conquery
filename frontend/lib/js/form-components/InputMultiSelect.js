// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select                     from 'react-select';
import classnames                 from 'classnames';
import { type FieldPropsType }    from 'redux-form';
import Dropzone                   from 'react-dropzone'

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
  isOver: boolean
};

const InputMultiSelect = (props: PropsType) => (
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
      style={{position: "relative", display: "block"}}
      activeClassName={'dropzone--over'}
      className={'dropzone'}
      onDrop={props.onDropFiles}
    >
      <Select
        name="form-field"
        options={props.options}
        value={props.input.value}
        onChange={(values) => props.input.onChange(values.map(v => v.value))}
        disabled={props.disabled}
        searchable
        multi
        placeholder={T.translate('reactSelect.placeholder')}
        backspaceToRemoveMessage={T.translate('reactSelect.backspaceToRemove')}
        clearAllText={T.translate('reactSelect.clearAll')}
        clearValueText={T.translate('reactSelect.clearValue')}
        noResultsText={T.translate('reactSelect.noResults')}
        onInputChange={props.onInputChange || function(value) { return value; }}
        isLoading={props.isLoading}
        className={props.className}
        matchPos="start"
      />
    </Dropzone>
  </label>
);

export default InputMultiSelect;
