// @flow

export const isProduction = process.env.NODE_ENV === 'production';

export const BASENAME = isProduction
  ? '/' // Possibly: Run under a subdomain on production
  : '/';

export const API_URL = isProduction
  ? 'http://localhost:8080/api'
  : 'http://localhost:8000/api';
