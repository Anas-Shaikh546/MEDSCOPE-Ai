"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authClient } from "@/lib/auth";
import { reportService } from "@/services/reportService";
import { ApiError } from "@/services/api";
import TrendChart from "@/components/TrendChart";
import type { TestTrend, TrendsResponse } from "@/types/report";

const CATEGORY_ORDER = ["CBC", "GLUCOSE", "LIPID", "KIDNEY", "LIVER", "THYROID", "VITAMINS", "URINE", "OTHER"];

export default function HealthPage() {
  const router = useRouter();
  const [trendsData, setTrendsData] = useState<TrendsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>("ALL");
  const [expandedTest, setExpandedTest] = useState<string | null>(null);

  const load = useCallback(async () => {
    if (!authClient.isAuthenticated()) {
      router.push("/login");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await reportService.getAllTrends();
      setTrendsData(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load health data");
    } finally {
      setLoading(false);
    }
  }, [router]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <main style={{ maxWidth: 700, margin: "80px auto" }}>Loading...</main>;

  const trends = trendsData?.trends ?? [];

  // Group by category, preserve controlled order
  const categories = ["ALL", ...CATEGORY_ORDER.filter((c) =>
    trends.some((t) => t.category === c)
  )];

  const visible = selectedCategory === "ALL"
    ? trends
    : trends.filter((t) => t.category === selectedCategory);

  const grouped: Record<string, TestTrend[]> = {};
  for (const t of visible) {
    if (!grouped[t.category]) grouped[t.category] = [];
    grouped[t.category].push(t);
  }

  return (
    <main style={{ maxWidth: 700, margin: "40px auto", padding: "0 16px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <h1 style={{ margin: 0 }}>My Health</h1>
        <a href="/dashboard" style={{ fontSize: 14 }}>← Reports</a>
      </div>

      {trends.length === 0 && !error && (
        <p style={{ marginTop: 24, color: "#6b7280" }}>
          No processed reports yet. Upload and process a report to see trends here.
        </p>
      )}

      {error && <p style={{ color: "crimson", marginTop: 12 }}>{error}</p>}

      {trends.length > 0 && (
        <>
          {/* Category filter */}
          <div style={{ display: "flex", gap: 8, flexWrap: "wrap", margin: "20px 0" }}>
            {categories.map((cat) => (
              <button
                key={cat}
                onClick={() => setSelectedCategory(cat)}
                style={{
                  padding: "4px 12px",
                  borderRadius: 20,
                  border: "1px solid #d1d5db",
                  background: selectedCategory === cat ? "#1d4ed8" : "#fff",
                  color: selectedCategory === cat ? "#fff" : "#374151",
                  cursor: "pointer",
                  fontSize: 13,
                }}
              >
                {cat}
              </button>
            ))}
          </div>

          {/* Grouped trend cards */}
          {Object.entries(grouped).sort(([a], [b]) =>
            CATEGORY_ORDER.indexOf(a) - CATEGORY_ORDER.indexOf(b)
          ).map(([category, tests]) => (
            <div key={category} style={{ marginBottom: 24 }}>
              <h3 style={{ fontSize: 13, color: "#6b7280", margin: "0 0 10px", letterSpacing: 1 }}>
                {category}
              </h3>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                {tests.map((test) => (
                  <TestTrendCard
                    key={test.canonicalName}
                    test={test}
                    expanded={expandedTest === test.canonicalName}
                    onToggle={() =>
                      setExpandedTest(prev =>
                        prev === test.canonicalName ? null : test.canonicalName
                      )
                    }
                  />
                ))}
              </div>
            </div>
          ))}

          <p style={{ fontSize: 12, color: "#9ca3af", marginTop: 24 }}>
            Trends are based on your processed reports. Dates marked with * are upload dates,
            not confirmed lab dates. This information does not constitute medical advice.
          </p>
        </>
      )}
    </main>
  );
}

function TestTrendCard({ test, expanded, onToggle }: {
  test: TestTrend;
  expanded: boolean;
  onToggle: () => void;
}) {
  const latest = test.observations[test.observations.length - 1];
  const trendColor = {
    INCREASING: "#2563eb",
    DECREASING: "#dc2626",
    STABLE: "#16a34a",
    FLUCTUATING: "#d97706",
    INSUFFICIENT_DATA: "#9ca3af",
    UNSUPPORTED: "#9ca3af",
  }[test.trend] ?? "#9ca3af";

  return (
    <div
      style={{
        border: "1px solid #e5e7eb",
        borderRadius: 8,
        padding: "12px 14px",
        cursor: "pointer",
      }}
      onClick={onToggle}
    >
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <div>
          <span style={{ fontWeight: 600, fontSize: 14 }}>{test.displayName}</span>
          {latest && (
            <span style={{ fontSize: 13, color: "#6b7280", marginLeft: 10 }}>
              {latest.value} {latest.unit}
              {!latest.dateIsConfirmed && " *"}
            </span>
          )}
        </div>
        <span style={{ fontSize: 12, color: trendColor, fontWeight: 600 }}>
          {test.trend.replace("_", " ")}
        </span>
      </div>

      {expanded && (
        <div style={{ marginTop: 12 }} onClick={(e) => e.stopPropagation()}>
          <TrendChart
            observations={test.observations}
            trend={test.trend}
            unit={test.defaultUnit}
          />

          {/* Observation table */}
          <table style={{ width: "100%", fontSize: 12, borderCollapse: "collapse", marginTop: 12 }}>
            <thead>
              <tr style={{ color: "#6b7280" }}>
                <th style={{ textAlign: "left", padding: "2px 6px 2px 0" }}>Date</th>
                <th style={{ textAlign: "left", padding: "2px 6px" }}>Value</th>
                <th style={{ textAlign: "left", padding: "2px 6px" }}>Range</th>
                <th style={{ textAlign: "left", padding: "2px 0" }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {test.observations.map((obs, i) => (
                <tr key={i} style={{ borderTop: "1px solid #f3f4f6" }}>
                  <td style={{ padding: "4px 6px 4px 0", color: obs.dateIsConfirmed ? "#111" : "#6b7280" }}>
                    {obs.date}{!obs.dateIsConfirmed ? " *" : ""}
                  </td>
                  <td style={{ padding: "4px 6px" }}>{obs.value} {obs.unit}</td>
                  <td style={{ padding: "4px 6px" }}>
                    {obs.referenceLow != null && obs.referenceHigh != null
                      ? `${obs.referenceLow} – ${obs.referenceHigh}`
                      : "—"}
                  </td>
                  <td style={{ padding: "4px 0" }}>{obs.status}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p style={{ fontSize: 11, color: "#9ca3af", margin: "6px 0 0" }}>
            * Upload date (lab date not set)
          </p>
        </div>
      )}
    </div>
  );
}