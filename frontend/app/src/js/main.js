import conquery from '../../../lib/js';
import exampleForm from "./forms/example-form";

require('../styles/styles.sass');
require('../images/favicon.png');

const isProduction = process.env.NODE_ENV === 'production';
const environment = {
  isProduction: isProduction,
  basename: isProduction
    ? '/' // Possibly: Run under a subpath on production
    : '/',
  apiUrl: isProduction
    ? 'http://localhost:8080/api'
    : 'http://localhost:8000/api'
};

// only provides the first selected form, not an order
const defaultForm = 'example';
const forms = {
  example: exampleForm,
};

conquery(environment, forms, defaultForm);
