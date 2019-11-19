// @flow

import fetch from "isomorphic-fetch";

import { getStoredAuthToken } from "../authorization";

type RequestType = {
  body?: Object | string,
  headers?: Object
};

function fetchJsonUnauthorized(
  url: string,
  request?: RequestType,
  rawBody?: boolean = false
) {
  const finalRequest = request
    ? {
        ...request,
        body: rawBody ? request.body : JSON.stringify(request.body),
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          ...request.headers
        }
      }
    : {
        method: "GET",
        headers: {
          Accept: "application/json"
        }
      };

  return fetch(url, finalRequest).then(
    response => {
      if (response.status >= 200 && response.status < 300)
        return response.json().catch(e => e);
      // Also handle empty responses
      // Reject other status
      else return response.json().then(Promise.reject.bind(Promise));
    },
    error => Promise.reject(error) // Network or connection failure
  );
}

function fetchJson(
  url: string,
  request?: RequestType,
  rawBody?: boolean = false
) {
  const authToken = getStoredAuthToken() || "";
  const finalRequest = {
    ...(request || {}),
    headers: {
      Authorization: `Bearer ${authToken}`,
      ...((request && request.headers) || {})
    }
  };

  return fetchJsonUnauthorized(url, finalRequest, rawBody);
}

export default fetchJson;
