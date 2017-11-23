import conquery from '../../../lib/js';
import exampleForm from "./forms/example-form";

require('../styles/styles.sass');
require('../images/favicon.png');

// only provides the first selected form, not an order
const defaultForm = 'example';
const forms = {
  example: exampleForm,
};

conquery(forms, defaultForm);
