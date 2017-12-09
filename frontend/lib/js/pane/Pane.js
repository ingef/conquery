// @flow

import React, { type Children } from 'react';
import PaneTabNavigation        from './PaneTabNavigation';

type PropsType = {
  type: 'left' | 'right',
  children?: Children
};

const Pane = (props: PropsType) => {
  return (
    <div className={`pane pane--${props.type}`}>
      <div className="pane__container">
        <PaneTabNavigation paneType={props.type} />
        { props.children }
      </div>
    </div>
  );
};


export default Pane;
