// @flow

import React                from 'react';
import classnames           from 'classnames';
import type { TabType }     from './reducer';

type PropsType = {
  onClickTab: Function,
  activeTab: string,
  tabs: TabType[]
};

const TabNavigation = (props: PropsType) => {
  return (
    <div className="tab-navigation">
      {
        props.tabs.map(({ label, tab }, i) => (
          <h2
            key={i}
            className={classnames(
              'tab-navigation__tab',
              {
                'tab-navigation__tab--active': props.activeTab === tab,
              }
            )}
            onClick={() => {
              if (tab !== props.activeTab)
                props.onClickTab(tab);
            }}
          >
            {label}
          </h2>
        ))
      }
    </div>
  );
};


export default TabNavigation;
