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
  isOver: boolean,
  allowDropFile?: ?boolean,
};

const InputMultiSelect = (props: PropsType) => {
  const allowDropFile = props.allowDropFile && !!props.onDropFiles

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
          options={props.options}
          value={props.input.value}
          onChange={(values) => props.input.onChange(values.map(v => v.value))}
          disabled={props.disabled}
          searchable
          multi
          placeholder={allowDropFile
            ? T.translate('reactSelect.dndPlaceholder')
            : T.translate('reactSelect.placeholder')
          }
          backspaceToRemoveMessage={T.translate('reactSelect.backspaceToRemove')}
          clearAllText={T.translate('reactSelect.clearAll')}
          clearValueText={T.translate('reactSelect.clearValue')}
          noResultsText={T.translate('reactSelect.noResults')}
          onInputChange={props.onInputChange || function(value) { return value; }}
          isLoading={props.isLoading}
          className={props.className}
        />
      </Dropzone>
    </label>
  )
};

export default InputMultiSelect;
