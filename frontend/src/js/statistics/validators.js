// @flow

import T           from 'i18n-react';
import { isEmpty } from '../common/helpers';

export const validateRequired = (value: any): ?string => {
  return isEmpty(value)
    ? T.translate('statistics.formValidation.isRequired')
    : null;
};

export const validatePositiveOrZero = (value: any) => {
  return value >= 0
    ? null
    : T.translate('statistics.formValidation.isRequired')
}

export const validateDateRange = (
  value: { minDate: string, maxDate: string }
): ?string => {
  if (!value.minDate || !value.maxDate)
    return T.translate('statistics.formValidation.isRequired');
  if (value.maxDate < value.minDate)
    return T.translate('statistics.formValidation.invalidDateRange');

  return null;
}

export const validateConceptGroupFilled = (
  group: { concepts: [] }[]
): ?string => {
  if (!group || group.length === 0)
    return T.translate('statistics.formValidation.isRequired');

  return group.some(e => e.concepts.length === 0 || e.concepts.some(c => !c))
    ? T.translate('statistics.formValidation.isRequired')
    : null;
};
