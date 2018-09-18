// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select                     from 'react-select';
import Dropzone                   from 'react-dropzone'
import classnames                 from 'classnames';
import { type FieldPropsType }    from 'redux-form';

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
  const { input, options } = props;
  const selected = options && options.filter(v => v.value === input.value);
  const defaultValue = options && options.filter(v => v.value === input.defaultValue);

  return (
    <label className={classnames(
      'input', {
        'input--value-changed':
          !isEmpty(input.value) && input.value !== input.defaultValue
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
          value={selected}
          defaultValue={defaultValue}
          options={options}
          isSearchable={false}
          onChange={
            field => field
              ? input.onChange(field.value)
              : input.onChange(null)
          }
          isClearable={input.clearable}
          isDisabled={!!props.disabled}
          placeholder={T.translate('reactSelect.placeholder')}
          noOptionsMessage={() => T.translate('reactSelect.noResults')}
          {...props.selectProps}
        />
      </Dropzone>
    </label>
  );
}


export default InputSelect;
