import conquery from '../../../lib/js/conquery';
import exampleForm from "./forms/example-form";

require('../styles/styles.sass');

// only provides the first selected form, not an order
const defaultForm = 'example';
const forms = {
  example: exampleForm,
};

conquery(forms, defaultForm);
