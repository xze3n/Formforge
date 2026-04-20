import { applicationApi } from "./applicationApi";
import { offlineStorage, PendingOperation } from "./offlineStorage";

export type SyncResult = {
  total: number;
  succeeded: number;
  failed: number;
  errors: string[];
};

async function processOperation(op: PendingOperation): Promise<void> {
  switch (op.kind) {
    case "CREATE": {
      const created = await applicationApi.create(op.input);
      // Remap the temp ID so subsequent queued ops target the real ID
      offlineStorage.remapId(op.tempId, created.id);
      break;
    }
    case "UPDATE": {
      await applicationApi.update(op.applicationId, op.input);
      break;
    }
    case "DELETE": {
      await applicationApi.delete(op.applicationId);
      break;
    }
  }
}

/**
 * Replay all queued offline operations against the server.
 * Operations are processed sequentially in the order they were created.
 */
export async function syncOfflineChanges(): Promise<SyncResult> {
  const queue = offlineStorage.getQueue();
  const result: SyncResult = {
    total: queue.length,
    succeeded: 0,
    failed: 0,
    errors: [],
  };

  for (const op of queue) {
    try {
      await processOperation(op);
      offlineStorage.dequeue(op.id);
      result.succeeded++;
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      result.errors.push(`${op.kind} (${op.id}): ${msg}`);
      result.failed++;
    }
  }

  return result;
}
