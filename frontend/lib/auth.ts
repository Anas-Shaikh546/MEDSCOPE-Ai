"use client";

import { api } from "@/services/api";
import type {
  AuthResponse,
  LoginPayload,
  RegisterPayload,
  UserProfile,
} from "@/types/auth";

const TOKEN_KEY = "medscope_access_token";

// Deliberately simple for Step 2: a thin wrapper around localStorage +
// the auth endpoints, kept out of individual pages so auth logic lives
// in one place.
export const authClient = {
  async register(payload: RegisterPayload): Promise<void> {
    await api.post<{ message: string }>("/auth/register", payload);
  },

  async login(payload: LoginPayload): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>("/auth/login", payload);
    if (typeof window !== "undefined") {
      window.localStorage.setItem(TOKEN_KEY, response.accessToken);
    }
    return response;
  },

  logout(): void {
    if (typeof window !== "undefined") {
      window.localStorage.removeItem(TOKEN_KEY);
    }
  },

  getToken(): string | null {
    if (typeof window === "undefined") return null;
    return window.localStorage.getItem(TOKEN_KEY);
  },

  isAuthenticated(): boolean {
    return this.getToken() !== null;
  },

  async fetchCurrentUser(): Promise<UserProfile> {
    const token = this.getToken();
    if (!token) {
      throw new Error("Not authenticated");
    }
    return api.get<UserProfile>("/users/me", token);
  },
};
