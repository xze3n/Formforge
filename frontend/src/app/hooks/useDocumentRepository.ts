import { useState, useReducer, useCallback, useEffect, useRef } from "react";
import { Document, CreateDocumentInput, UpdateDocumentInput } from "../types/document";
import { documentApi } from "../services/documentApi";
import { offlineStorage } from "../services/offlineStorage";
import { networkService } from "../services/networkService";

type Action =
  | { type: "SET_DOCUMENTS"; payload: Document[] }
  | { type: "ADD_DOCUMENT"; payload: Document }
  | { type: "UPDATE_DOCUMENT"; payload: Document }
  | { type: "DELETE_DOCUMENT"; payload: number };

const docsReducer = (state: Document[], action: Action): Document[] => {
  switch (action.type) {
    case "SET_DOCUMENTS":
      return action.payload;
    case "ADD_DOCUMENT":
      return [...state, action.payload];
    case "UPDATE_DOCUMENT":
      return state.map(d => Number(d.id) === Number(action.payload.id) ? action.payload : d);
    case "DELETE_DOCUMENT":
      return state.filter(d => Number(d.id) !== Number(action.payload));
    default:
      return state;
  }
};

const cacheKey = (applicationId: number) => `ff_offline_documents_${applicationId}`;

function readCache(applicationId: number): Document[] {
  try {
    const raw = localStorage.getItem(cacheKey(applicationId));
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function writeCache(applicationId: number, docs: Document[]) {
  try {
    localStorage.setItem(cacheKey(applicationId), JSON.stringify(docs));
  } catch {
    // storage quota exceeded — ignore
  }
}
export const useDocumentRepository = (applicationId: number) => {
  const [documents, dispatch] = useReducer(docsReducer, []);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const loadedOnlineRef = useRef(false);

  const clearError = useCallback(() => setError(null), []);

  const fetchFromServer = useCallback(() => {
    if (!applicationId) return;
    documentApi.getByApplicationId(applicationId)
      .then(docs => {
        dispatch({ type: "SET_DOCUMENTS", payload: docs });
        writeCache(applicationId, docs);
        loadedOnlineRef.current = true;
      })
      .catch(() => {
        // ignore — keep existing state
      });
  }, [applicationId]);

  // ── Load on mount ──────────────────────────────────────────────────
  useEffect(() => {
    if (!applicationId) return;
    loadedOnlineRef.current = false;
    setLoading(true);
    documentApi.getByApplicationId(applicationId)
      .then(docs => {
        dispatch({ type: "SET_DOCUMENTS", payload: docs });
        writeCache(applicationId, docs);
        loadedOnlineRef.current = true;
      })
      .catch(() => {
        const cached = readCache(applicationId);
        dispatch({ type: "SET_DOCUMENTS", payload: cached });
      })
      .finally(() => setLoading(false));
  }, [applicationId]);

  // ── Re-fetch when back online or after sync completes ──────────────
  useEffect(() => {
    if (!applicationId) return;
    // After sync, replace any temp-ID documents with real server data
    const onSyncEnd = () => fetchFromServer();
    window.addEventListener("ff:sync:end", onSyncEnd);

    // If initial load was from cache (offline), retry when connection restored.
    // Only fetch directly when there are no pending ops — if ops exist,
    // performSync will fire ff:sync:end after processing them, which triggers
    // fetchFromServer above. Fetching here concurrently would race with the
    // in-flight DELETE_DOC mutations and potentially restore deleted docs.
    const unsubNetwork = networkService.subscribe((online) => {
      if (online && !loadedOnlineRef.current && offlineStorage.pendingCount === 0) {
        fetchFromServer();
      }
    });

    return () => {
      window.removeEventListener("ff:sync:end", onSyncEnd);
      unsubNetwork();
    };
  }, [applicationId, fetchFromServer]);

  // ── CRUD ───────────────────────────────────────────────────────────
  const add = useCallback(async (input: CreateDocumentInput): Promise<Document> => {
    clearError();
    // If the parent application has a temp ID it doesn't exist on the server yet —
    // go straight to offline storage to avoid an FK violation.
    if (applicationId < 0) {
      const tempDoc = offlineStorage.addDocument(applicationId, input);
      offlineStorage.enqueue({ kind: "CREATE_DOC", applicationId, input, tempId: tempDoc.id });
      dispatch({ type: "ADD_DOCUMENT", payload: tempDoc });
      return tempDoc;
    }
    try {
      const created = await documentApi.create(applicationId, input);
      dispatch({ type: "ADD_DOCUMENT", payload: created });
      writeCache(applicationId, [...readCache(applicationId), created]);
      return created;
    } catch {
      // Offline fallback — create a local document with a temp ID and queue for sync
      const tempDoc = offlineStorage.addDocument(applicationId, input);
      offlineStorage.enqueue({ kind: "CREATE_DOC", applicationId, input, tempId: tempDoc.id });
      dispatch({ type: "ADD_DOCUMENT", payload: tempDoc });
      return tempDoc;
    }
  }, [applicationId, clearError]);

  const update = useCallback(async (id: number, input: UpdateDocumentInput): Promise<Document> => {
    clearError();
    // Temp-ID document only lives in local storage — patch locally, no server call.
    if (id < 0) {
      const cached = readCache(applicationId);
      const idx = cached.findIndex(d => Number(d.id) === id);
      if (idx === -1) throw new Error("Document not found in local cache");
      const patched = { ...cached[idx], ...input };
      cached[idx] = patched;
      writeCache(applicationId, cached);
      offlineStorage.saveDocuments(applicationId, cached);
      // Update the queued CREATE_DOC input so sync sends the latest data
      const queue = offlineStorage.getQueue();
      const createOp = queue.find(op => op.kind === "CREATE_DOC" && op.tempId === id);
      if (createOp && createOp.kind === "CREATE_DOC") {
        offlineStorage.dequeue(createOp.id);
        offlineStorage.enqueue({ kind: "CREATE_DOC", applicationId, input: { ...createOp.input, ...input }, tempId: id });
      }
      dispatch({ type: "UPDATE_DOCUMENT", payload: patched });
      return patched;
    }
    try {
      const updated = await documentApi.update(id, input);
      dispatch({ type: "UPDATE_DOCUMENT", payload: updated });
      writeCache(applicationId, readCache(applicationId).map(d => Number(d.id) === id ? updated : d));
      return updated;
    } catch {
      // Offline fallback — apply patch locally and queue for sync
      const cached = readCache(applicationId);
      const idx = cached.findIndex(d => Number(d.id) === id);
      if (idx === -1) throw new Error("Document not found in local cache");
      const patched = { ...cached[idx], ...input };
      cached[idx] = patched;
      writeCache(applicationId, cached);
      offlineStorage.saveDocuments(applicationId, cached);
      offlineStorage.enqueue({ kind: "UPDATE_DOC", documentId: id, input });
      dispatch({ type: "UPDATE_DOCUMENT", payload: patched });
      return patched;
    }
  }, [applicationId, clearError]);

  const remove = useCallback(async (id: number): Promise<void> => {
    clearError();
    // Optimistic local update first so UI responds immediately
    dispatch({ type: "DELETE_DOCUMENT", payload: id });
    const updatedCache = readCache(applicationId).filter(d => Number(d.id) !== id);
    writeCache(applicationId, updatedCache);
    offlineStorage.saveDocuments(applicationId, updatedCache);
    // Temp-ID document was never persisted — remove the queued CREATE_DOC and stop.
    if (id < 0) {
      const queue = offlineStorage.getQueue();
      const createOp = queue.find(op => op.kind === "CREATE_DOC" && op.tempId === id);
      if (createOp) offlineStorage.dequeue(createOp.id);
      return;
    }
    try {
      await documentApi.delete(id);
    } catch {
      // Server unreachable — queue delete for later sync
      offlineStorage.enqueue({ kind: "DELETE_DOC", documentId: id });
    }
  }, [applicationId, clearError]);

  return { documents, loading, error, clearError, add, update, remove };
};
