// @flow

import React                from 'react';
import T                    from 'i18n-react';

import { CloseIconButton }  from '../button';


type PropsType = {
  children?: Element[],
  closeModal: Function,
  doneButton: boolean,
};

class Modal extends React.Component {
  props: PropsType;

  constructor(props: PropsType) {
    super(props);

    (this:any).boundKeyUpHandler = this._onKeyUp.bind(this);
  }

  componentDidMount() {
    window.addEventListener('keyup', (this:any).boundKeyUpHandler);
  }

  componentWillUnmount() {
    window.removeEventListener('keyup', (this:any).boundKeyUpHandler);
  }

  _onKeyUp(e: Object) {
    switch (e.keyCode) {
    case 27: // Esc key
      this.props.closeModal();
      break;
    default:
      break;
    }
  }

  _closeMaybe(e: Object) {
    if (e.nativeEvent.target.className.indexOf('modal__center') !== -1)
      this.props.closeModal();
  }

  render() {
    const closeButton = this.props.doneButton
      ? <button
          type="button"
          className="modal__close-button btn btn--transparent btn--small"
          onClick={this.props.closeModal}
        >
          { T.translate('common.done') }
        </button>
      : <CloseIconButton
          className="modal__close-button"
          onClick={this.props.closeModal}
        />;

    return (
      <div className="modal" onClick={this._closeMaybe.bind(this)}>
        <div className="modal__center">
          <div className="modal__content">
            { closeButton }
            { this.props.children }
          </div>
        </div>
      </div>
    );
  }
}

export default Modal;
