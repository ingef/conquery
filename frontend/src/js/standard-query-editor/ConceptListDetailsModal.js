// @flow

import React                     from 'react';
import T                         from 'i18n-react';

import { Modal }                 from '../modal';

import { ScrollableList }        from '../scrollable-list';

type PropsType = {
  headline: String,
  conceptTreeRoot: String,
  items: String[],
  onCloseModal: Function,
};

const ConceptListDetailsModal = (props: PropsType) => {
 return (
  <Modal closeModal={props.onCloseModal} doneButton>
    <div className="concept-list-details-modal">
      <h3>
        { props.headline }
      </h3>
      <div className="concept-list-details-modal__section">
        <label className="input">
          <span className="input-label">
            { T.translate('conceptListDetailsModal.conceptTreeRoot') }
          </span>
          <ScrollableList items={[props.conceptTreeRoot]} minWidth fullWidth />
        </label>
      </div>
      <div className="concept-list-details-modal__section">
        <label className="input">
          <span className="input-label">
            { T.translate('conceptListDetailsModal.conceptCodes') }
          </span>
          <ScrollableList items={props.items} maxVisibleItems={5} minWidth fullWidth />
        </label>
      </div>
    </div>
  </Modal>
  )
}

export default ConceptListDetailsModal;
