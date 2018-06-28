// @flow

import React                from 'react';
import classnames           from 'classnames';
import T                    from 'i18n-react';
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
        Object.values(props.tabs).map(({ label, key }) => (
          <h2
            key={key}
            className={classnames(
              'tab-navigation__tab',
              {
                'tab-navigation__tab--active': props.activeTab === key,
              }
            )}
            onClick={() => {
              if (key !== props.activeTab)
                props.onClickTab(key);
            }}
          >
            {T.translate(label)}
          </h2>
        ))
      }
    </div>
  );
};


export default TabNavigation;
