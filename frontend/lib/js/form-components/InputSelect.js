// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select                     from 'react-select';
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
};

const InputSelect = (props: PropsType) => {
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
    </label>
  );
}


export default InputSelect;
