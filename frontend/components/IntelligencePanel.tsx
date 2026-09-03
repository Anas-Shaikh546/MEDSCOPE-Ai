import { useState } from "react";
import type { Insight, InsightGenerationSummary, InsightPriority } from "@/types/intelligence";

interface IntelligencePanelProps {
  reportId: number;
  insights: InsightGenerationSummary | null;
  loading: boolean;
  onGenerate: () => void;
}

function priorityBadgeColor(priority: InsightPriority): string {
  switch (priority) {
    case "HIGH": return "linear-gradient(135deg, #f093fb 0%, #f5576c 100%)";
    case "MODERATE": return "linear-gradient(135deg, #fa709a 0%, #fee140 100%)";
    case "LOW": return "linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)";
    case "INFORMATIONAL": return "linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)";
    default: return "linear-gradient(135deg, #d299c2 0%, #fef9d7 100%)";
  }
}

function priorityLabel(priority: InsightPriority): string {
  switch (priority) {
    case "HIGH": return "High Priority";
    case "MODERATE": return "Moderate";
    case "LOW": return "Low Priority";
    case "INFORMATIONAL": return "Info";
    default: return priority;
  }
}

function InsightCard({ insight }: { insight: Insight }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="glass-card-light fade-in" style={{
      padding: "20px",
      marginBottom: "16px",
      transition: "all 0.3s ease",
      cursor: "pointer"
    }} onClick={() => setExpanded(!expanded)}>
      <div style={{ display: "flex", alignItems: "start", gap: "16px", marginBottom: "12px" }}>
        <div style={{
          background: priorityBadgeColor(insight.priority),
          color: "#fff",
          padding: "6px 14px",
          borderRadius: "20px",
          fontSize: "12px",
          fontWeight: "700",
          whiteSpace: "nowrap",
          boxShadow: "0 4px 15px rgba(0,0,0,0.15)"
        }}>
          {priorityLabel(insight.priority)}
        </div>
        <div style={{ flex: 1 }}>
          <h4 style={{ margin: 0, fontSize: "16px", fontWeight: "600", color: "#2d3748", marginBottom: "8px" }}>
            {insight.title}
          </h4>
        </div>
      </div>

      <p style={{ margin: "12px 0", fontSize: "14px", color: "#4a5568", lineHeight: "1.7" }}>
        {insight.description}
      </p>

      {insight.followUpQuestions && (
        <div style={{ marginTop: "16px", paddingTop: "16px", borderTop: "1px solid rgba(0,0,0,0.08)" }}>
          <div style={{
            display: "flex",
            alignItems: "center",
            gap: "8px",
            color: "#667eea",
            fontSize: "13px",
            fontWeight: "600",
            marginBottom: expanded ? "12px" : "0"
          }}>
            <span style={{ transition: "transform 0.3s ease", transform: expanded ? "rotate(90deg)" : "rotate(0deg)" }}>▶</span>
            Follow-up questions
          </div>
          {expanded && (
            <ul style={{
              marginTop: "12px",
              paddingLeft: "24px",
              fontSize: "13px",
              color: "#4a5568",
              lineHeight: "1.7"
            }}>
              {insight.followUpQuestions.split("\n").map((q, i) => (
                <li key={i} style={{ marginBottom: "8px" }}>{q}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      <div style={{ marginTop: "16px", display: "flex", alignItems: "center", gap: "12px" }}>
        <div style={{
          fontSize: "12px",
          color: "#718096",
          background: "rgba(102, 126, 234, 0.1)",
          padding: "4px 12px",
          borderRadius: "12px"
        }}>
          Confidence: {Math.round(insight.confidence * 100)}%
        </div>
        <div style={{
          fontSize: "11px",
          color: "#a0aec0",
        }}>
          {new Date(insight.createdAt).toLocaleString()}
        </div>
      </div>
    </div>
  );
}

export default function IntelligencePanel({
  reportId,
  insights,
  loading,
  onGenerate
}: IntelligencePanelProps) {
  const groupedInsights: Record<InsightPriority, Insight[]> = {
    HIGH: [],
    MODERATE: [],
    LOW: [],
    INFORMATIONAL: []
  };

  if (insights?.insights) {
    insights.insights.forEach(insight => {
      groupedInsights[insight.priority].push(insight);
    });
  }

  const hasAnyInsights = insights && insights.insightCount > 0;

  return (
    <div style={{ marginTop: "24px" }}>
      <div style={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        marginBottom: "20px"
      }}>
        <h3 style={{ margin: 0, fontSize: "20px", fontWeight: "700", color: "#fff", textShadow: "0 2px 10px rgba(0,0,0,0.1)" }}>
          🧠 Intelligence Insights
        </h3>
        <button
          onClick={(e) => {
            e.stopPropagation();
            onGenerate();
          }}
          disabled={loading}
          className="btn-primary btn-small"
        >
          {loading ? "⏳ Generating..." : insights ? "🔄 Regenerate" : "✨ Generate Insights"}
        </button>
      </div>

      {loading && (
        <div className="glass-card-light fade-in" style={{
          padding: "60px 40px",
          textAlign: "center",
        }}>
          <div style={{
            width: "60px",
            height: "60px",
            border: "4px solid rgba(102, 126, 234, 0.2)",
            borderTop: "4px solid #667eea",
            borderRadius: "50%",
            margin: "0 auto 20px",
            animation: "spin 1s linear infinite"
          }}></div>
          <p style={{ color: "#4a5568", fontSize: "15px", fontWeight: "500" }}>
            Analyzing longitudinal health data...
          </p>
        </div>
      )}

      {!loading && !hasAnyInsights && (
        <div className="glass-card-light fade-in" style={{
          padding: "60px 40px",
          textAlign: "center",
        }}>
          <div style={{ fontSize: "48px", marginBottom: "16px" }}>🔍</div>
          <p style={{ color: "#4a5568", fontSize: "15px", fontWeight: "500", marginBottom: "8px" }}>
            No insights available yet
          </p>
          <p style={{ color: "#718096", fontSize: "13px" }}>
            Click Generate to analyze this report with historical context
          </p>
        </div>
      )}

      {!loading && hasAnyInsights && (
        <div>
          {(["HIGH", "MODERATE", "LOW", "INFORMATIONAL"] as InsightPriority[]).map(priority => {
            const priorityInsights = groupedInsights[priority];
            if (priorityInsights.length === 0) return null;

            return (
              <div key={priority} style={{ marginBottom: "32px" }}>
                <h4 style={{
                  fontSize: "13px",
                  fontWeight: "700",
                  color: "rgba(255, 255, 255, 0.9)",
                  textTransform: "uppercase",
                  letterSpacing: "0.08em",
                  marginBottom: "16px",
                  textShadow: "0 2px 10px rgba(0,0,0,0.1)"
                }}>
                  {priorityLabel(priority)} ({priorityInsights.length})
                </h4>
                {priorityInsights.map(insight => (
                  <InsightCard key={insight.id} insight={insight} />
                ))}
              </div>
            );
          })}
        </div>
      )}

      <style jsx>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
    </div>
  );
}
