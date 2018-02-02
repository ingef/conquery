// @flow

import React                from 'react';

import { getStoredAuthToken } from '../authorization';

type PropsType = {
  url: string,
  label?: string,
  className?: string,
};

const DownloadButton = (props: PropsType) => {
  const authToken = getStoredAuthToken();

  const href = `${props.url}?access_token=${encodeURIComponent(authToken || '')}`;

  return (
    <a
      href={href}
      className={props.className}
    >
      <i
        className="fa fa-download"
      /> {props.label}
    </a>
  );
};

export default DownloadButton;
