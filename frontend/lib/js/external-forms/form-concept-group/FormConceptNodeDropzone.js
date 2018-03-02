// @flow

import React              from 'react';

import {
  CATEGORY_TREE_NODE,
} from '../../common/constants/dndTypes';

import { Dropzone }       from '../../form-components';

type PropsType = {
  dropzoneText: string,
  onDrop: Function
}

const FormConceptNodeDropzone = (props: PropsType) => {
  const QueryDropzone = Dropzone(null, [CATEGORY_TREE_NODE], props.onDrop);

  return (
    <QueryDropzone
      className="externalForms__concept-node-dropzone"
      containsItem={false}
      dropzoneText={props.dropzoneText}
    />
  );
};

export default FormConceptNodeDropzone;
