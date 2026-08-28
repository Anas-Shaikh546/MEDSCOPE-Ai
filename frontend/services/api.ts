import type { ApiErrorResponse } from "@/types/auth";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

interface RequestOptions extends RequestInit {
  authToken?: string | null;
}

/**
 * Single entry point for every backend request. Nothing in the app should
 * call fetch("http://localhost:8080/...") directly - that gets painful the
 * moment the base URL, auth header, or error shape needs to change once.
 */
async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { authToken, headers, ...rest } = options;

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
      ...headers,
    },
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const errorBody = (await response.json()) as ApiErrorResponse;
      message = errorBody.message ?? message;
    } catch {
      // response body wasn't JSON - fall back to the generic message
    }
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string, authToken?: string | null) =>
    request<T>(path, { method: "GET", authToken }),

  post: <T>(path: string, body: unknown, authToken?: string | null) =>
    request<T>(path, { method: "POST", body: JSON.stringify(body), authToken }),
};
