// @flow

import React                from 'react';
import classnames           from 'classnames';

type PropsType = {
  className?: string,
  message: string,
};

const ErrorMessage = ({ className, message }: PropsType) => {
  return (
    <p className={classnames('error-message', className)}>
      {message}
    </p>
  );
};

export default ErrorMessage;
