const BASE = "/api/admin";

export interface AuditLogEntry {
  id: number;
  userId: number | null;
  username: string | null;
  userRole: string | null;
  action: string;
  resourceType: string | null;
  resourceId: string | null;
  details: string | null;
  ipAddress: string | null;
  success: boolean;
  createdAt: string;
}

export interface ObservationEntry {
  id: number;
  userId: number;
  username: string | null;
  reason: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  detectedAt: string;
  resolved: boolean;
  resolvedAt: string | null;
  resolvedBy: string | null;
  triggerAction: string | null;
  occurrenceCount: number;
}

export interface AdminPage<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

function adminHeaders(): HeadersInit {
  try {
    const raw = sessionStorage.getItem("formforge_user");
    if (!raw) return {};
    const user = JSON.parse(raw);
    return {
      "X-User-Id":   String(user.id),
      "X-User-Name": user.username,
      "X-User-Role": user.role,
    };
  } catch {
    return {};
  }
}

async function adminFetch<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    ...init,
    headers: { "Content-Type": "application/json", ...adminHeaders(), ...(init?.headers ?? {}) },
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `Request failed: ${res.status}`);
  }
  return res.json();
}

export function fetchAuditLogs(params: {
  page?: number;
  size?: number;
  userId?: number;
  action?: string;
}): Promise<AdminPage<AuditLogEntry>> {
  const qs = new URLSearchParams();
  if (params.page  != null) qs.set("page",   String(params.page));
  if (params.size  != null) qs.set("size",   String(params.size));
  if (params.userId) qs.set("userId", String(params.userId));
  if (params.action) qs.set("action", params.action);
  return adminFetch(`${BASE}/audit-logs?${qs}`);
}

export function fetchObservations(params: {
  page?: number;
  size?: number;
  unresolvedOnly?: boolean;
}): Promise<AdminPage<ObservationEntry>> {
  const qs = new URLSearchParams();
  if (params.page          != null) qs.set("page",          String(params.page));
  if (params.size          != null) qs.set("size",          String(params.size));
  if (params.unresolvedOnly != null) qs.set("unresolvedOnly", String(params.unresolvedOnly));
  return adminFetch(`${BASE}/observations?${qs}`);
}

export function fetchUnresolvedCount(): Promise<{ unresolved: number }> {
  return adminFetch(`${BASE}/observations/count`);
}

export function resolveObservation(id: number): Promise<ObservationEntry> {
  return adminFetch(`${BASE}/observations/${id}/resolve`, { method: "POST" });
}
