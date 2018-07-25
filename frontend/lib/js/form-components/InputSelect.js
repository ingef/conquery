// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select                     from 'react-select';
import Dropzone                   from 'react-dropzone'
import classnames                 from 'classnames';
import { type FieldPropsType }    from 'redux-form';

import 'react-select/dist/react-select.css';

import { isEmpty }                from '../common/helpers';
import { type SelectOptionsType } from '../common/types/backend';
import InfoTooltip                from '../tooltip/InfoTooltip';

type PropsType = FieldPropsType & {
  label: string,
  options: SelectOptionsType,
  disabled?: boolean,
  selectProps?: Object,
  tooltip?: string,
  onDropFiles?: Function,
  allowDropFile?: ?boolean,
};

const InputSelect = (props: PropsType) => {
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
          value={props.input.value}
          options={props.options}
          searchable={false}
          onChange={
            field => field
              ? props.input.onChange(field.value)
              : props.input.onChange(null)
          }
          clearable={props.input.clearable}
          disabled={!!props.disabled}
          placeholder={allowDropFile
            ? T.translate('reactSelect.dndPlaceholder')
            : T.translate('reactSelect.placeholder')
          }
          backspaceToRemoveMessage={T.translate('reactSelect.backspaceToRemove')}
          clearAllText={T.translate('reactSelect.clearAll')}
          clearValueText={T.translate('reactSelect.clearValue')}
          noResultsText={T.translate('reactSelect.noResults')}
          {...props.selectProps}
        />
      </Dropzone>
    </label>
  );
}


export default InputSelect;
