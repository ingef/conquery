// @flow

import { connect }          from 'react-redux';
import { type Dispatch }    from 'redux-thunk';

import { isEmpty }          from '../common/helpers';
import * as actions         from './actions';
import HoverableBase        from './HoverableBase';

// Decorates a component with a hoverable node.
// On mouse enter, additional infos about the component are saved in the state
// The Tooltip (and potential other components) might then update their view.
// On mouse leave, the infos are cleared from the state again
const AdditionalInfoHoverable = (Component: any) => {
  const mapStateToProps = () => ({});

  const mapDispatchToProps = (dispatch: Dispatch, ownProps) => ({
    onDisplayAdditionalInfos: () => {
      const node = ownProps.node;

      if (!node.additionalInfos && isEmpty(node.matchingEntries)) return;

      // Whitelist the data we pass (especially: don't pass all children)
      const additionalInfos = {
        label: ownProps.node.label,
        description: node.description,
        matchingEntries: node.matchingEntries,
        dateRange: node.dateRange,
        additionalInfos: node.additionalInfos,
      };

      dispatch(actions.displayAdditionalInfos(additionalInfos))
    },
  });

  return connect(mapStateToProps, mapDispatchToProps)(HoverableBase(Component));
};

export default AdditionalInfoHoverable;
