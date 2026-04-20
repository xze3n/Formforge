import {
  Application,
  CreateApplicationInput,
  UpdateApplicationInput,
} from "../types/application";
import { EnumValues } from "./enumApi";

const APPS_KEY = "ff_offline_applications";
const QUEUE_KEY = "ff_offline_queue";
const ENUMS_KEY = "ff_offline_enums";
const TEMP_ID_KEY = "ff_offline_temp_id";

// ── Pending operation types ────────────────────────────────────────────
export type PendingOperation =
  | { id: string; kind: "CREATE"; input: CreateApplicationInput; tempId: number; timestamp: number }
  | { id: string; kind: "UPDATE"; applicationId: number; input: UpdateApplicationInput; timestamp: number }
  | { id: string; kind: "DELETE"; applicationId: number; timestamp: number };

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
    const idx = apps.findIndex((a) => a.id === id);
    if (idx === -1) return null;
    apps[idx] = { ...apps[idx], ...input };
    this.saveApplications(apps);
    return apps[idx];
  },

  deleteApplication(id: number) {
    const apps = this.getApplications().filter((a) => a.id !== id);
    this.saveApplications(apps);
  },

  /** Replace a temp ID with a real server-assigned ID */
  remapId(tempId: number, realId: number) {
    const apps = this.getApplications();
    const idx = apps.findIndex((a) => a.id === tempId);
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
    }
    if (changed) this.saveQueue(queue);
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
};
