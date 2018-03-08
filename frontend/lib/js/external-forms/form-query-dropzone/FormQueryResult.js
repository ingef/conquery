// @flow

import React               from 'react';

import { CloseIconButton } from '../../button';

type PropsType = {
  queryResult?: Object,
  className?: string,
  onDelete: Function,
};

const FormQueryResult = (props: PropsType) => {
  return props.queryResult && (
    <div className={props.className}>
      <CloseIconButton
        onClick={props.onDelete}
      />
      { props.queryResult.label || props.queryResult.id }
    </div>
  );
};

export default FormQueryResult;
