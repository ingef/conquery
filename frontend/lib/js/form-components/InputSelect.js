// @flow

import React                      from 'react';
import T                          from 'i18n-react';
import Select                     from 'react-select';
import classnames                 from 'classnames';
import { type FieldPropsType }    from 'redux-form';

import { isEmpty }                from '../common/helpers';
import { type SelectOptionsType } from '../common/types';
import InfoTooltip                from '../tooltip/InfoTooltip';

type PropsType = FieldPropsType & {
  label: string,
  options: SelectOptionsType,
  disabled?: boolean,
  selectProps?: Object,
  tooltip?: string,
};

const InputSelect = (props: PropsType) => {
  return (
    <label className={classnames(
      'input', {
        'input--value-changed': !isEmpty(props.input.value)
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
        placeholder={T.translate('reactSelect.placeholder')}
        backspaceToRemoveMessage={T.translate('reactSelect.backspaceToRemove')}
        clearAllText={T.translate('reactSelect.clearAll')}
        clearValueText={T.translate('reactSelect.clearValue')}
        noResultsText={T.translate('reactSelect.noResults')}
        {...props.selectProps}
      />
    </label>
  );
}


export default InputSelect;
