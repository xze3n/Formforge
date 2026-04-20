import { useState, useReducer, useCallback, useEffect, useRef } from "react";
import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";
import { applicationApi } from "../services/applicationApi";
import { networkService } from "../services/networkService";
import { offlineStorage } from "../services/offlineStorage";
import { syncOfflineChanges } from "../services/syncService";

type Action =
  | { type: "SET_APPLICATIONS"; payload: Application[] }
  | { type: "ADD_APPLICATION"; payload: Application }
  | { type: "UPDATE_APPLICATION"; payload: Application }
  | { type: "DELETE_APPLICATION"; payload: number };

const appsReducer = (state: Application[], action: Action): Application[] => {
  switch (action.type) {
    case "SET_APPLICATIONS":
      return action.payload;
    case "ADD_APPLICATION":
      return [...state, action.payload];
    case "UPDATE_APPLICATION":
      return state.map(app =>
        app.id === action.payload.id ? action.payload : app
      );
    case "DELETE_APPLICATION":
      return state.filter(app => app.id !== action.payload);
    default:
      return state;
  }
};

/**
 * Custom hook to manage applications via the backend API with offline support.
 * When the server is unreachable, operations are persisted to localStorage
 * and queued for synchronisation once the connection is restored.
 */
export const useApplicationRepository = () => {
  const [applications, dispatch] = useReducer(appsReducer, []);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);
  const syncingRef = useRef(false);

  const clearError = useCallback(() => setError(null), []);

  // ── Load data on mount (server-first, fall back to cache) ──────────
  useEffect(() => {
    applicationApi.getAll()
      .then(apps => {
        dispatch({ type: "SET_APPLICATIONS", payload: apps });
        offlineStorage.saveApplications(apps);
      })
      .catch(() => {
        // Server unreachable – load cached data
        const cached = offlineStorage.getApplications();
        dispatch({ type: "SET_APPLICATIONS", payload: cached });
      })
      .finally(() => setLoading(false));
  }, []);

  // ── Auto-sync when back online ─────────────────────────────────────
  const performSync = useCallback(async () => {
    if (syncingRef.current) return;
    if (offlineStorage.pendingCount === 0) return;

    syncingRef.current = true;
    setSyncing(true);
    window.dispatchEvent(new Event("ff:sync:start"));
    try {
      const result = await syncOfflineChanges();
      if (result.failed > 0) {
        setError(`Sync: ${result.failed} operation(s) failed`);
      }
      // Refresh from server after sync
      try {
        const apps = await applicationApi.getAll();
        dispatch({ type: "SET_APPLICATIONS", payload: apps });
        offlineStorage.saveApplications(apps);
      } catch {
        // server went away again mid-sync; keep local state
      }
    } finally {
      syncingRef.current = false;
      setSyncing(false);
      window.dispatchEvent(new Event("ff:sync:end"));
    }
  }, []);

  useEffect(() => {
    const unsub = networkService.subscribe((online) => {
      if (online) performSync();
    });
    // Also try on mount in case we come back online before subscribe
    if (networkService.online) performSync();
    return unsub;
  }, [performSync]);

  // ── CRUD operations ────────────────────────────────────────────────
  const getAll = useCallback((): Application[] => {
    return applications;
  }, [applications]);

  const getById = useCallback((id: number): Application | undefined => {
    return applications.find(app => app.id === id);
  }, [applications]);

  const add = useCallback(async (input: CreateApplicationInput): Promise<Application> => {
    clearError();
    if (networkService.online) {
      try {
        const created = await applicationApi.create(input);
        dispatch({ type: "ADD_APPLICATION", payload: created });
        offlineStorage.saveApplications([...offlineStorage.getApplications(), created]);
        return created;
      } catch {
        // Request failed – fall through to offline path
      }
    }
    // Offline path
    const app = offlineStorage.addApplication(input);
    offlineStorage.enqueue({ kind: "CREATE", input, tempId: app.id });
    dispatch({ type: "ADD_APPLICATION", payload: app });
    return app;
  }, [clearError]);

  const update = useCallback(
    async (id: number, input: UpdateApplicationInput): Promise<Application> => {
      clearError();
      if (networkService.online) {
        try {
          const updated = await applicationApi.update(id, input);
          dispatch({ type: "UPDATE_APPLICATION", payload: updated });
          offlineStorage.updateApplication(id, input);
          return updated;
        } catch {
          // fall through
        }
      }
      // Offline path
      const updated = offlineStorage.updateApplication(id, input);
      if (!updated) throw new Error("Application not found in local cache");
      offlineStorage.enqueue({ kind: "UPDATE", applicationId: id, input });
      dispatch({ type: "UPDATE_APPLICATION", payload: updated });
      return updated;
    },
    [clearError]
  );

  const remove = useCallback(async (id: number): Promise<boolean> => {
    clearError();
    if (networkService.online) {
      try {
        await applicationApi.delete(id);
        dispatch({ type: "DELETE_APPLICATION", payload: id });
        offlineStorage.deleteApplication(id);
        return true;
      } catch {
        // fall through
      }
    }
    // Offline path
    offlineStorage.deleteApplication(id);
    offlineStorage.enqueue({ kind: "DELETE", applicationId: id });
    dispatch({ type: "DELETE_APPLICATION", payload: id });
    return true;
  }, [clearError]);

  return {
    applications,
    loading,
    error,
    syncing,
    clearError,
    getAll,
    getById,
    add,
    update,
    remove,
    delete: remove,
  };
};
