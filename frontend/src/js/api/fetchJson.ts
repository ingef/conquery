import axios, { AxiosRequestConfig } from "axios";
import { getStoredAuthToken } from "../authorization/helper";

export async function fetchJsonUnauthorized(
  url: string,
  request?: Partial<AxiosRequestConfig>,
  rawBody: boolean = false
) {
  const finalRequest: AxiosRequestConfig = request
    ? {
        ...request,
        url,
        data: rawBody ? request.data : JSON.stringify(request.data),
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          ...request.headers
        }
      }
    : {
        url,
        method: "GET",
        headers: {
          Accept: "application/json"
        }
      };

  try {
    const response = await axios({
      ...finalRequest,
      validateStatus: () => true // Don't ever reject the promise for special status codes
    });

    if (response.status >= 200 && response.status < 300) {
      // Also handle empty responses
      return response.data;
    } else {
      // Reject other status
      try {
        return Promise.reject({ status: response.status, ...response.data });
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
  request?: Partial<AxiosRequestConfig>,
  rawBody: boolean = false
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
