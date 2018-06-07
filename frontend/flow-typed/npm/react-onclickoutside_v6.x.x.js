// flow-typed signature: 5d55033f60a033d464e91c262148abb4
// flow-typed version: 48c34babb0/react-onclickoutside_v6.x.x/flow_>=v0.54.1

declare module 'react-onclickoutside' {
  declare export type OnClickOutsideProps = {
    eventTypes?: Array<string>,
    outsideClickIgnoreClass?: string,
    preventDefault?: boolean,
    stopPropagation?: boolean
  };

  declare module.exports: <P, S>(
    BaseComponent: Class<React$Component<P, S>>,
    config?: { excludeScrollbar?: boolean }
  ) => React$ComponentType<P & OnClickOutsideProps & {
    excludeScrollbar?: boolean,
    disableOnClickOutside?: boolean
  }>;
}
