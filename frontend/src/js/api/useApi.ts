import axios, { AxiosRequestConfig, AxiosResponse } from "axios";
import { useContext, useEffect, useRef } from "react";
import { useHistory } from "react-router-dom";

import { AuthTokenContext } from "../authorization/AuthTokenProvider";
import {
  getCachedEtagResource,
  storeEtagResource,
} from "../common/helpers/etagCache";
import { isIDPEnabled, isLoginDisabled } from "../environment";

export const useApiUnauthorized = <T>(
  requestConfig: Partial<AxiosRequestConfig> = {},
) => {
  return (finalRequestConfig: Partial<AxiosRequestConfig> = {}): Promise<T> =>
    fetchJsonUnauthorized({
      ...requestConfig,
      ...finalRequestConfig,
    });
};

interface CustomCacheConfig {
  etagCacheKey?: string;
}

export const useApi = <T>(requestConfig: Partial<AxiosRequestConfig> = {}) => {
  const history = useHistory();
  const { authToken } = useContext(AuthTokenContext);

  // In order to always have the up to date token,
  // especially when polling for a long time within nested loops
  const authTokenRef = useRef<string>(authToken);
  useEffect(
    function updateRef() {
      authTokenRef.current = authToken;
    },
    [authToken],
  );

  return async (
    finalRequestConfig: Partial<AxiosRequestConfig> = {},
    cacheConfig: CustomCacheConfig = {},
  ): Promise<T> => {
    try {
      const axiosRequestConfig = {
        ...requestConfig,
        ...finalRequestConfig,
        headers: {
          Authorization: `Bearer ${authTokenRef.current}`,
          ...(requestConfig.headers || {}),
          ...(finalRequestConfig.headers || {}),
        },
      };

      const response = await fetchJsonUnauthorized(
        axiosRequestConfig,
        cacheConfig,
      );

      return response;
    } catch (error) {
      if (
        !isIDPEnabled &&
        !isLoginDisabled &&
        (error as { status?: number }).status &&
        (error as { status?: number }).status === 401
      ) {
        history.push("/login");
      }

      console.error(error);
      throw error;
    }
  };
};

async function getCacheHeaders(cacheConfig: CustomCacheConfig) {
  if (!cacheConfig.etagCacheKey) return {};

  const item = await getCachedEtagResource(cacheConfig.etagCacheKey);

  return item ? { "If-None-Match": item.etag } : {};
}

function handleResponseCache(
  response: AxiosResponse<Object>,
  cacheConfig: CustomCacheConfig,
) {
  const { etagCacheKey } = cacheConfig;
  const { etag } = response.headers;

  if (etagCacheKey && etag) {
    storeEtagResource(etagCacheKey, etag, response.data);
  }
}

async function getCachedResponse(cacheConfig: CustomCacheConfig) {
  if (!cacheConfig.etagCacheKey)
    return Promise.reject(
      "Status 304, but did not find an etag identifier to find resource by.",
    );

  const responseFromCache = await getCachedEtagResource(
    cacheConfig.etagCacheKey,
  );

  return responseFromCache
    ? responseFromCache.resource
    : Promise.reject(
        "Status 304, but did not find a stored resource to return.",
      );
}

export async function fetchJsonUnauthorized(
  request?: Partial<AxiosRequestConfig>,
  cacheConfig: CustomCacheConfig = {},
  rawBody: boolean = false,
) {
  const finalRequest: AxiosRequestConfig = request
    ? {
        ...request,
        data: rawBody ? request.data : JSON.stringify(request.data),
        headers: {
          Accept: "application/json",
          "Content-Type": "application/json",
          ...(await getCacheHeaders(cacheConfig)),
          ...request.headers,
        },
      }
    : {
        method: "GET",
        headers: {
          Accept: "application/json",
        },
      };

  const response = await axios({
    ...finalRequest,
    validateStatus: () => true, // Don't ever reject the promise for special status codes
  });

  if (response.status === 304) {
    return getCachedResponse(cacheConfig);
  }

  if (
    response.status >= 200 &&
    response.status < 300 &&
    // Also handle empty responses
    (!response.data || (!!response.data && !response.data.error))
  ) {
    handleResponseCache(response, cacheConfig);

    return response.data;
  } else {
    // Reject other status
    try {
      return Promise.reject({ status: response.status, ...response.data });
    } catch (e) {
      return Promise.reject({ status: response.status });
    }
  }
}
