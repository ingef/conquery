import React                from 'react';
import PropTypes            from 'prop-types';
import T                    from 'i18n-react';
import { connect }          from 'react-redux';

import { Modal }            from '../../modal';
import {
  deletePreviousQuery
}                           from '../list/actions';
import {
  deletePreviousQueryModalClose,
}                           from './actions';


const DeletePreviousQueryModal = (props) => {
  return !!props.queryId && (
    <Modal closeModal={props.onCloseModal}>
      <div className="delete-previous-query-modal">
        <h3 className="delete-previous-query-modal__headline">
          { T.translate('deletePreviousQueryModal.areYouSure') }
        </h3>
        <button
          className="delete-previous-query-modal__btn btn btn--transparent"
          onClick={props.onCloseModal}
        >
          { T.translate('common.cancel') }
        </button>
        <button
          className="delete-previous-query-modal__btn btn btn--primary"
          onClick={props.onDeletePreviousQuery}
        >
          { T.translate('common.delete') }
        </button>
      </div>
    </Modal>
  );
};

DeletePreviousQueryModal.propTypes = {
  queryId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  onCloseModal: PropTypes.func.isRequired,
  onDeletePreviousQuery: PropTypes.func.isRequired,
};

const mapStateToProps = (state) => ({
  queryId: state.deletePreviousQueryModal.queryId,
});

const mapDispatchToProps = (dispatch) => ({
  onCloseModal: () => dispatch(deletePreviousQueryModalClose()),
  onDeletePreviousQuery: (datasetId, queryId) => {
    dispatch(deletePreviousQueryModalClose());
    dispatch(deletePreviousQuery(datasetId, queryId));
  }
});

const mergeProps = (stateProps, dispatchProps, ownProps) => ({
  ...stateProps,
  ...dispatchProps,
  ...ownProps,
  onDeletePreviousQuery: () =>
    dispatchProps.onDeletePreviousQuery(ownProps.datasetId, stateProps.queryId),
});

export default connect(mapStateToProps, mapDispatchToProps, mergeProps)(DeletePreviousQueryModal);
