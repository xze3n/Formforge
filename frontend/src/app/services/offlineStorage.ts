import {
  Application,
  CreateApplicationInput,
  UpdateApplicationInput,
} from "../types/application";
import { Document, CreateDocumentInput, UpdateDocumentInput } from "../types/document";
import { EnumValues } from "./enumApi";

const APPS_KEY = "ff_offline_applications";
const QUEUE_KEY = "ff_offline_queue";
const ENUMS_KEY = "ff_offline_enums";
const TEMP_ID_KEY = "ff_offline_temp_id";
const DOC_TEMP_ID_KEY = "ff_offline_doc_temp_id";

// ── Pending operation types ────────────────────────────────────────────
export type PendingOperation =
  | { id: string; kind: "CREATE"; input: CreateApplicationInput; tempId: number; timestamp: number }
  | { id: string; kind: "UPDATE"; applicationId: number; input: UpdateApplicationInput; timestamp: number }
  | { id: string; kind: "DELETE"; applicationId: number; timestamp: number }
  | { id: string; kind: "CREATE_DOC"; applicationId: number; input: CreateDocumentInput; tempId: number; timestamp: number }
  | { id: string; kind: "UPDATE_DOC"; documentId: number; input: UpdateDocumentInput; timestamp: number }
  | { id: string; kind: "DELETE_DOC"; documentId: number; timestamp: number };

// ── Helpers ────────────────────────────────────────────────────────────
function read<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function write<T>(key: string, value: T) {
  localStorage.setItem(key, JSON.stringify(value));
}

let nextTempId: number = read<number>(TEMP_ID_KEY, -1);

function allocTempId(): number {
  const id = nextTempId;
  nextTempId -= 1;
  write(TEMP_ID_KEY, nextTempId);
  return id;
}

let nextDocTempId: number = read<number>(DOC_TEMP_ID_KEY, -1);

function allocDocTempId(): number {
  const id = nextDocTempId;
  nextDocTempId -= 1;
  write(DOC_TEMP_ID_KEY, nextDocTempId);
  return id;
}

function uuid(): string {
  return crypto.randomUUID();
}

// ── Application cache ──────────────────────────────────────────────────
export const offlineStorage = {
  // ── Applications ────────────────────────────────────────────────────
  getApplications(): Application[] {
    return read<Application[]>(APPS_KEY, []);
  },

  saveApplications(apps: Application[]) {
    write(APPS_KEY, apps);
  },

  addApplication(input: CreateApplicationInput): Application {
    const tempId = allocTempId();
    const now = new Date();
    const app: Application = {
      id: tempId,
      type: input.type,
      academicYear: input.academicYear,
      semester: input.semester,
      status: input.status ?? "Draft",
      createdAt: `${now.getMonth() + 1}/${now.getDate()}/${now.getFullYear()}`,
    };
    const apps = this.getApplications();
    apps.push(app);
    this.saveApplications(apps);
    return app;
  },

  updateApplication(id: number, input: UpdateApplicationInput): Application | null {
    const apps = this.getApplications();
    const idx = apps.findIndex((a) => Number(a.id) === Number(id));
    if (idx === -1) return null;
    apps[idx] = { ...apps[idx], ...input };
    this.saveApplications(apps);
    return apps[idx];
  },

  deleteApplication(id: number) {
    const apps = this.getApplications().filter((a) => Number(a.id) !== Number(id));
    this.saveApplications(apps);
  },

  /** Replace a temp ID with a real server-assigned ID */
  remapId(tempId: number, realId: number) {
    const apps = this.getApplications();
    const idx = apps.findIndex((a) => Number(a.id) === Number(tempId));
    if (idx !== -1) {
      apps[idx] = { ...apps[idx], id: realId };
      this.saveApplications(apps);
    }
    // Also remap any queued operations that reference the tempId
    const queue = this.getQueue();
    let changed = false;
    for (const op of queue) {
      if (op.kind === "UPDATE" && op.applicationId === tempId) {
        op.applicationId = realId;
        changed = true;
      }
      if (op.kind === "DELETE" && op.applicationId === tempId) {
        op.applicationId = realId;
        changed = true;
      }
      if (op.kind === "CREATE_DOC" && op.applicationId === tempId) {
        op.applicationId = realId;
        changed = true;
      }
    }
    if (changed) this.saveQueue(queue);
    // Also move document cache from temp-keyed bucket to real-ID bucket
    const tempDocs = this.getDocuments(tempId);
    if (tempDocs.length > 0) {
      const realDocs = this.getDocuments(realId);
      this.saveDocuments(realId, [...realDocs, ...tempDocs]);
      localStorage.removeItem(this.docKey(tempId));
    }
  },

  // ── Operation queue ─────────────────────────────────────────────────
  getQueue(): PendingOperation[] {
    return read<PendingOperation[]>(QUEUE_KEY, []);
  },

  saveQueue(queue: PendingOperation[]) {
    write(QUEUE_KEY, queue);
  },

  enqueue(op: Omit<PendingOperation, "id" | "timestamp">) {
    const queue = this.getQueue();
    queue.push({ ...op, id: uuid(), timestamp: Date.now() } as PendingOperation);
    this.saveQueue(queue);
  },

  dequeue(id: string) {
    const queue = this.getQueue().filter((o) => o.id !== id);
    this.saveQueue(queue);
  },

  clearQueue() {
    localStorage.removeItem(QUEUE_KEY);
  },

  get pendingCount(): number {
    return this.getQueue().length;
  },

  // ── Enums cache ─────────────────────────────────────────────────────
  getEnums(): EnumValues | null {
    return read<EnumValues | null>(ENUMS_KEY, null);
  },

  saveEnums(enums: EnumValues) {
    write(ENUMS_KEY, enums);
  },

  // ── Document cache ───────────────────────────────────────────────────
  docKey(applicationId: number): string {
    return `ff_offline_documents_${applicationId}`;
  },

  getDocuments(applicationId: number): Document[] {
    return read<Document[]>(this.docKey(applicationId), []);
  },

  saveDocuments(applicationId: number, docs: Document[]) {
    write(this.docKey(applicationId), docs);
  },

  addDocument(applicationId: number, input: CreateDocumentInput): Document {
    const tempId = allocDocTempId();
    const now = new Date();
    const doc: Document = {
      id: tempId,
      applicationId,
      name: input.name,
      type: input.type,
      description: input.description ?? null,
      dateAdded: `${now.getMonth() + 1}/${now.getDate()}/${now.getFullYear()}`,
      verified: false,
      notes: input.notes ?? null,
    };
    const docs = this.getDocuments(applicationId);
    docs.push(doc);
    this.saveDocuments(applicationId, docs);
    return doc;
  },

  updateDocument(applicationId: number, id: number, input: UpdateDocumentInput): Document | null {
    const docs = this.getDocuments(applicationId);
    const idx = docs.findIndex((d) => Number(d.id) === Number(id));
    if (idx === -1) return null;
    docs[idx] = { ...docs[idx], ...input };
    this.saveDocuments(applicationId, docs);
    return docs[idx];
  },

  deleteDocument(applicationId: number, id: number) {
    const docs = this.getDocuments(applicationId).filter((d) => Number(d.id) !== Number(id));
    this.saveDocuments(applicationId, docs);
  },

  remapDocId(applicationId: number, tempId: number, realId: number) {
    const docs = this.getDocuments(applicationId);
    const idx = docs.findIndex((d) => Number(d.id) === Number(tempId));
    if (idx !== -1) {
      docs[idx] = { ...docs[idx], id: realId };
      this.saveDocuments(applicationId, docs);
    }
    // Also remap queued operations targeting the temp doc ID
    const queue = this.getQueue();
    let changed = false;
    for (const op of queue) {
      if (op.kind === "UPDATE_DOC" && op.documentId === tempId) {
        op.documentId = realId;
        changed = true;
      }
      if (op.kind === "DELETE_DOC" && op.documentId === tempId) {
        op.documentId = realId;
        changed = true;
      }
    }
    if (changed) this.saveQueue(queue);
  },
};
