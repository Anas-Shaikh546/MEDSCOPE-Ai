"use client";

import type { TimelineObservation, TrendDirection } from "@/types/report";

interface TrendChartProps {
  observations: TimelineObservation[];
  trend: TrendDirection;
  unit: string | null;
}

const TREND_COLOR: Record<TrendDirection, string> = {
  INCREASING: "#2563eb",
  DECREASING: "#dc2626",
  STABLE: "#16a34a",
  FLUCTUATING: "#d97706",
  INSUFFICIENT_DATA: "#9ca3af",
  UNSUPPORTED: "#9ca3af",
};

const TREND_LABEL: Record<TrendDirection, string> = {
  INCREASING: "↑ Increasing",
  DECREASING: "↓ Decreasing",
  STABLE: "→ Stable",
  FLUCTUATING: "↕ Fluctuating",
  INSUFFICIENT_DATA: "Not enough data",
  UNSUPPORTED: "Mixed units",
};

/**
 * Dependency-free inline SVG trend chart. Intentionally minimal -
 * shows the shape of change over time, not clinical significance.
 * No diagnosis language, no interpretation (that's Step 5's job).
 */
export default function TrendChart({ observations, trend, unit }: TrendChartProps) {
  if (observations.length === 0) return null;

  const W = 280;
  const H = 80;
  const PAD = 12;

  const values = observations.map((o) => o.value);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const color = TREND_COLOR[trend];

  const toX = (i: number) =>
    PAD + (i / Math.max(observations.length - 1, 1)) * (W - PAD * 2);
  const toY = (v: number) =>
    PAD + (1 - (v - min) / range) * (H - PAD * 2);

  const points = observations
    .map((o, i) => `${toX(i)},${toY(o.value)}`)
    .join(" ");

  return (
    <div>
      <svg width={W} height={H} style={{ display: "block" }}>
        {/* grid lines */}
        <line x1={PAD} y1={PAD} x2={PAD} y2={H - PAD} stroke="#e5e7eb" strokeWidth={1} />
        <line x1={PAD} y1={H - PAD} x2={W - PAD} y2={H - PAD} stroke="#e5e7eb" strokeWidth={1} />

        {/* trend line */}
        <polyline
          points={points}
          fill="none"
          stroke={color}
          strokeWidth={2}
          strokeLinejoin="round"
          strokeLinecap="round"
        />

        {/* data points */}
        {observations.map((o, i) => (
          <circle key={i} cx={toX(i)} cy={toY(o.value)} r={3} fill={color} />
        ))}
      </svg>

      <div style={{ display: "flex", justifyContent: "space-between", fontSize: 11, color: "#6b7280", marginTop: 2 }}>
        <span>{observations[0]?.date}</span>
        <span style={{ color, fontWeight: 600 }}>{TREND_LABEL[trend]}</span>
        <span>{observations[observations.length - 1]?.date}</span>
      </div>

      {/* min/max labels */}
      <div style={{ fontSize: 11, color: "#6b7280", marginTop: 2 }}>
        {min.toFixed(1)} – {max.toFixed(1)}{unit ? ` ${unit}` : ""}
      </div>
    </div>
  );
}