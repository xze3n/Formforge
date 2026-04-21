import { useState, useReducer, useCallback, useEffect } from "react";
import { Document, CreateDocumentInput, UpdateDocumentInput } from "../types/document";
import { documentApi } from "../services/documentApi";

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
      return state.map(d => d.id === action.payload.id ? action.payload : d);
    case "DELETE_DOCUMENT":
      return state.filter(d => d.id !== action.payload);
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

  const clearError = useCallback(() => setError(null), []);

  // ── Load on mount ──────────────────────────────────────────────────
  useEffect(() => {
    if (!applicationId) return;
    setLoading(true);
    documentApi.getByApplicationId(applicationId)
      .then(docs => {
        dispatch({ type: "SET_DOCUMENTS", payload: docs });
        writeCache(applicationId, docs);
      })
      .catch(() => {
        const cached = readCache(applicationId);
        dispatch({ type: "SET_DOCUMENTS", payload: cached });
      })
      .finally(() => setLoading(false));
  }, [applicationId]);

  // ── CRUD ───────────────────────────────────────────────────────────
  const add = useCallback(async (input: CreateDocumentInput): Promise<Document> => {
    clearError();
    try {
      const created = await documentApi.create(applicationId, input);
      dispatch({ type: "ADD_DOCUMENT", payload: created });
      writeCache(applicationId, [...readCache(applicationId), created]);
      return created;
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to add document";
      setError(msg);
      throw err;
    }
  }, [applicationId, clearError]);

  const update = useCallback(async (id: number, input: UpdateDocumentInput): Promise<Document> => {
    clearError();
    try {
      const updated = await documentApi.update(id, input);
      dispatch({ type: "UPDATE_DOCUMENT", payload: updated });
      writeCache(applicationId, readCache(applicationId).map(d => d.id === id ? updated : d));
      return updated;
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to update document";
      setError(msg);
      throw err;
    }
  }, [applicationId, clearError]);

  const remove = useCallback(async (id: number): Promise<void> => {
    clearError();
    try {
      await documentApi.delete(id);
      dispatch({ type: "DELETE_DOCUMENT", payload: id });
      writeCache(applicationId, readCache(applicationId).filter(d => d.id !== id));
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to delete document";
      setError(msg);
      throw err;
    }
  }, [applicationId, clearError]);

  return { documents, loading, error, clearError, add, update, remove };
};
