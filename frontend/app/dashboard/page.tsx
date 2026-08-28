"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient } from "@/lib/auth";
import type { UserProfile } from "@/types/auth";

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authClient.isAuthenticated()) {
      router.push("/login");
      return;
    }

    authClient
      .fetchCurrentUser()
      .then(setUser)
      .catch(() => {
        authClient.logout();
        router.push("/login");
      });
  }, [router]);

  function handleLogout() {
    authClient.logout();
    router.push("/login");
  }

  if (!user) {
    return <main style={{ maxWidth: 480, margin: "80px auto" }}>Loading...</main>;
  }

  return (
    <main style={{ maxWidth: 480, margin: "80px auto" }}>
      <h1>Welcome, {user.firstName}</h1>
      <p>Email: {user.email}</p>
      <p>Account created: {new Date(user.createdAt).toLocaleDateString()}</p>

      <hr style={{ margin: "24px 0" }} />

      {/* Step 3 territory - not built yet on purpose */}
      <p>No reports yet. Upload your first medical report.</p>

      {error && <p style={{ color: "crimson" }}>{error}</p>}

      <button onClick={handleLogout} style={{ marginTop: 24 }}>
        Log out
      </button>
    </main>
  );
}
