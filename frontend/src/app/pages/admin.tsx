import { useState, useEffect, useCallback } from "react";
import {
  fetchAuditLogs, fetchObservations, resolveObservation, fetchUnresolvedCount,
  type AuditLogEntry, type ObservationEntry,
} from "../services/adminService";
import { useAuth } from "../hooks/useAuth";
import { Navigate } from "react-router";
import { Shield, ScrollText, Eye, CheckCircle, RefreshCw, ChevronLeft, ChevronRight } from "lucide-react";

// ── Severity badge ──────────────────────────────────────────────────────────

const SEVERITY_STYLE: Record<string, string> = {
  LOW:      "bg-blue-100  text-blue-800",
  MEDIUM:   "bg-yellow-100 text-yellow-800",
  HIGH:     "bg-orange-100 text-orange-800",
  CRITICAL: "bg-red-100   text-red-800",
};

function SeverityBadge({ s }: { s: string }) {
  return (
    <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold ${SEVERITY_STYLE[s] ?? "bg-gray-100 text-gray-700"}`}>
      {s}
    </span>
  );
}

// ── Helpers ─────────────────────────────────────────────────────────────────

function fmt(ts: string) {
  return new Date(ts).toLocaleString(undefined, {
    year: "numeric", month: "short", day: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  });
}

function Pagination({ page, total, onChange }: { page: number; total: number; onChange: (p: number) => void }) {
  return (
    <div className="flex items-center gap-2 mt-3 justify-end text-sm text-gray-600">
      <button disabled={page === 0} onClick={() => onChange(page - 1)}
        className="p-1 rounded hover:bg-gray-100 disabled:opacity-40">
        <ChevronLeft className="w-4 h-4" />
      </button>
      <span>Page {page + 1} / {Math.max(total, 1)}</span>
      <button disabled={page >= total - 1} onClick={() => onChange(page + 1)}
        className="p-1 rounded hover:bg-gray-100 disabled:opacity-40">
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  );
}

// ── Audit Logs Tab ───────────────────────────────────────────────────────────

function AuditLogsTab() {
  const [logs, setLogs]     = useState<AuditLogEntry[]>([]);
  const [total, setTotal]   = useState(0);
  const [page, setPage]     = useState(0);
  const [action, setAction] = useState("");
  const [loading, setLoading] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    fetchAuditLogs({ page, size: 50, action: action || undefined })
      .then(r => { setLogs(r.content); setTotal(r.totalPages); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page, action]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="flex flex-wrap items-center gap-3 mb-4">
        <input
          placeholder="Filter by action (e.g. LOGIN_FAILURE)"
          value={action}
          onChange={e => { setAction(e.target.value.toUpperCase()); setPage(0); }}
          className="border border-gray-300 rounded-lg px-3 py-1.5 text-sm w-72 focus:outline-none focus:ring-2 focus:ring-purple-400"
        />
        <button onClick={load}
          className="flex items-center gap-1 text-sm px-3 py-1.5 rounded-lg border border-gray-300 hover:bg-gray-50">
          <RefreshCw className="w-3.5 h-3.5" /> Refresh
        </button>
        <span className="text-xs text-gray-500 ml-auto">{loading ? "Loading…" : `${total} page(s)`}</span>
      </div>

      <div className="overflow-x-auto rounded-xl border border-gray-200">
        <table className="w-full text-xs">
          <thead className="bg-gray-50 text-gray-700 uppercase tracking-wide">
            <tr>
              {["Timestamp","User","Role","Action","Resource","IP","Status"].map(h => (
                <th key={h} className="px-3 py-2 text-left font-semibold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {logs.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">No entries</td></tr>
            ) : logs.map(l => (
              <tr key={l.id} className="hover:bg-gray-50">
                <td className="px-3 py-2 whitespace-nowrap text-gray-500">{fmt(l.createdAt)}</td>
                <td className="px-3 py-2 font-medium text-gray-800">{l.username ?? <span className="italic text-gray-400">anonymous</span>}</td>
                <td className="px-3 py-2">
                  {l.userRole ? (
                    <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${l.userRole === "ADMIN" ? "bg-purple-100 text-purple-700" : "bg-gray-100 text-gray-600"}`}>
                      {l.userRole}
                    </span>
                  ) : "—"}
                </td>
                <td className="px-3 py-2">
                  <span className="font-mono text-gray-700">{l.action}</span>
                  {l.details && <div className="text-gray-400 max-w-xs truncate">{l.details}</div>}
                </td>
                <td className="px-3 py-2 text-gray-500">{l.resourceType ? `${l.resourceType}${l.resourceId ? ` #${l.resourceId}` : ""}` : "—"}</td>
                <td className="px-3 py-2 font-mono text-gray-400">{l.ipAddress ?? "—"}</td>
                <td className="px-3 py-2">
                  {l.success
                    ? <span className="text-green-600 font-semibold">✓</span>
                    : <span className="text-red-600 font-semibold">✗</span>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} total={total} onChange={setPage} />
    </div>
  );
}

// ── Observations Tab ─────────────────────────────────────────────────────────

function ObservationsTab() {
  const [entries, setEntries]       = useState<ObservationEntry[]>([]);
  const [total, setTotal]           = useState(0);
  const [page, setPage]             = useState(0);
  const [unresolvedOnly, setUnresolved] = useState(true);
  const [loading, setLoading]       = useState(false);
  const [resolving, setResolving]   = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    fetchObservations({ page, size: 50, unresolvedOnly })
      .then(r => { setEntries(r.content); setTotal(r.totalPages); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [page, unresolvedOnly]);

  useEffect(() => { load(); }, [load]);

  // Auto-refresh every 5 s so new detections appear in real time
  useEffect(() => {
    const id = setInterval(load, 5_000);
    return () => clearInterval(id);
  }, [load]);

  const handleResolve = async (id: number) => {
    setResolving(id);
    try {
      await resolveObservation(id);
      load();
    } finally {
      setResolving(null);
    }
  };

  return (
    <div>
      <div className="flex items-center gap-4 mb-4">
        <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer select-none">
          <input type="checkbox" checked={unresolvedOnly} onChange={e => { setUnresolved(e.target.checked); setPage(0); }}
            className="w-4 h-4 accent-purple-600" />
          Show unresolved only
        </label>
        <button onClick={load}
          className="flex items-center gap-1 text-sm px-3 py-1.5 rounded-lg border border-gray-300 hover:bg-gray-50">
          <RefreshCw className="w-3.5 h-3.5" /> Refresh
        </button>
        <span className="text-xs text-gray-500 ml-auto">{loading ? "Loading…" : ""}</span>
      </div>

      <div className="overflow-x-auto rounded-xl border border-gray-200">
        <table className="w-full text-xs">
          <thead className="bg-gray-50 text-gray-700 uppercase tracking-wide">
            <tr>
              {["User","Reason","Severity","Detected","Count","Status","Action"].map(h => (
                <th key={h} className="px-3 py-2 text-left font-semibold">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {entries.length === 0 ? (
              <tr><td colSpan={7} className="text-center py-8 text-gray-400">No suspicious activity detected</td></tr>
            ) : entries.map(e => (
              <tr key={e.id} className={e.resolved ? "bg-gray-50 opacity-60" : "hover:bg-red-50/30"}>
                <td className="px-3 py-2 font-medium text-gray-800">{e.username ?? `uid:${e.userId}`}</td>
                <td className="px-3 py-2">
                  <span className="font-mono text-gray-700">{e.reason.replace(/_/g, " ")}</span>
                  {e.details && <div className="text-gray-500 text-[11px] mt-0.5 max-w-xs">{e.details}</div>}
                  {e.aiExplanation && (
                    <div className="mt-1.5 bg-purple-50 border border-purple-200 rounded p-1.5 text-[11px] text-purple-800 italic max-w-sm">
                      <span className="not-italic font-semibold text-purple-600 mr-1">AI:</span>
                      {e.aiExplanation}
                    </div>
                  )}
                  {!e.aiExplanation && !e.resolved && (
                    <div className="mt-1 text-[10px] text-gray-400 italic">AI analysis pending…</div>
                  )}
                </td>
                <td className="px-3 py-2"><SeverityBadge s={e.severity} /></td>
                <td className="px-3 py-2 whitespace-nowrap text-gray-500">{fmt(e.detectedAt)}</td>
                <td className="px-3 py-2 text-center font-bold text-gray-700">{e.occurrenceCount}</td>
                <td className="px-3 py-2">
                  {e.resolved
                    ? <span className="text-green-600 font-semibold flex items-center gap-1"><CheckCircle className="w-3 h-3" /> Resolved</span>
                    : <span className="text-red-600 font-semibold">Active</span>}
                </td>
                <td className="px-3 py-2">
                  {!e.resolved && (
                    <button
                      disabled={resolving === e.id}
                      onClick={() => handleResolve(e.id)}
                      className="px-2 py-1 text-xs rounded bg-green-600 text-white hover:bg-green-700 disabled:opacity-50">
                      {resolving === e.id ? "…" : "Resolve"}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Pagination page={page} total={total} onChange={setPage} />
    </div>
  );
}

// ── Page ─────────────────────────────────────────────────────────────────────

export function AdminPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<"logs" | "observations">("observations");
  const [unresolved, setUnresolved] = useState(0);

  useEffect(() => {
    fetchUnresolvedCount().then(r => setUnresolved(r.unresolved)).catch(() => {});
  }, []);

  if (!user || user.role !== "ADMIN") return <Navigate to="/" replace />;

  return (
    <main className="max-w-7xl mx-auto px-4 py-8">
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <Shield className="w-7 h-7 text-purple-600" />
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Security Admin</h1>
          <p className="text-sm text-gray-500">Audit trail and threat-detection observation list</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="flex gap-0">
          <button
            onClick={() => setTab("observations")}
            className={`flex items-center gap-2 px-5 py-3 text-sm font-medium border-b-2 transition-colors ${
              tab === "observations"
                ? "border-purple-600 text-purple-700"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            <Eye className="w-4 h-4" />
            Suspicious Users
            {unresolved > 0 && (
              <span className="ml-1 inline-flex items-center justify-center w-5 h-5 rounded-full bg-red-500 text-white text-[10px] font-bold">
                {unresolved > 99 ? "99+" : unresolved}
              </span>
            )}
          </button>
          <button
            onClick={() => setTab("logs")}
            className={`flex items-center gap-2 px-5 py-3 text-sm font-medium border-b-2 transition-colors ${
              tab === "logs"
                ? "border-purple-600 text-purple-700"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            <ScrollText className="w-4 h-4" />
            Audit Logs
          </button>
        </nav>
      </div>

      {tab === "observations" ? <ObservationsTab /> : <AuditLogsTab />}
    </main>
  );
}
