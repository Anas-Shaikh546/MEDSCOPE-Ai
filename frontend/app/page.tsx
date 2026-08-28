import Link from "next/link";

export default function HomePage() {
  return (
    <main style={{ maxWidth: 480, margin: "80px auto", textAlign: "center" }}>
      <h1>MedScope AI</h1>
      <p>Medical report analysis - foundation build (Steps 1-2).</p>
      <div style={{ display: "flex", gap: 12, justifyContent: "center", marginTop: 24 }}>
        <Link href="/login">Log in</Link>
        <Link href="/register">Register</Link>
      </div>
    </main>
  );
}
