// @flow

import fetch from "isomorphic-fetch";

import { getStoredAuthToken } from "../authorization";

type RequestType = {
  body?: Object | string,
  headers?: Object
};

export async function fetchJsonUnauthorized(
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

  try {
    const response = await fetch(url, finalRequest);

    if (response.status >= 200 && response.status < 300) {
      // Also handle empty responses
      return response.json().catch(e => e);
    } else {
      // Reject other status
      try {
        const parsed = await response.json();

        return Promise.reject({ status: response.status, ...parsed });
      } catch (e) {
        return Promise.reject({ status: response.status });
      }
    }
  } catch (e) {
    return Promise.reject(e); // Network or connection failure
  }
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
