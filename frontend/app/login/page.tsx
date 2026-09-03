"use client";

import { useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { authClient } from "@/lib/auth";
import { ApiError } from "@/services/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await authClient.login({ email, password });
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Login failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", padding: "20px" }}>
      <div className="glass-card fade-in" style={{ width: "100%", maxWidth: "400px", padding: "40px" }}>
        <div style={{ textAlign: "center", marginBottom: "32px" }}>
          <h1 style={{ fontSize: "32px", fontWeight: "700", margin: "0 0 8px 0", textShadow: "0 2px 10px rgba(0,0,0,0.1)" }}>
            Welcome Back
          </h1>
          <p style={{ fontSize: "14px", color: "rgba(255,255,255,0.8)", margin: 0 }}>
            Sign in to your MedScope account
          </p>
        </div>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "16px" }}>
          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "8px", color: "rgba(255,255,255,0.9)" }}>
              Email
            </label>
            <input
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              className="input-field"
            />
          </div>

          <div>
            <label style={{ display: "block", fontSize: "13px", fontWeight: "600", marginBottom: "8px", color: "rgba(255,255,255,0.9)" }}>
              Password
            </label>
            <input
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="input-field"
            />
          </div>

          {error && (
            <div style={{ padding: "12px", background: "rgba(239, 68, 68, 0.2)", border: "1px solid rgba(239, 68, 68, 0.4)", borderRadius: "8px" }}>
              <p style={{ color: "#fff", margin: 0, fontSize: "13px" }}>⚠️ {error}</p>
            </div>
          )}

          <button type="submit" disabled={submitting} className="btn-primary" style={{ marginTop: "8px" }}>
            {submitting ? "⏳ Signing in..." : "Sign In"}
          </button>
        </form>

        <div style={{ marginTop: "24px", textAlign: "center" }}>
          <p style={{ fontSize: "14px", color: "rgba(255,255,255,0.7)" }}>
            Don't have an account?{" "}
            <a href="/register" style={{ color: "#fff", fontWeight: "600", textDecoration: "none" }}>
              Sign up
            </a>
          </p>
        </div>
      </div>
    </main>
  );
}
