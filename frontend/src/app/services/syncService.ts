import { applicationApi } from "./applicationApi";
import { documentApi } from "./documentApi";
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
    case "CREATE_DOC": {
      const created = await documentApi.create(op.applicationId, op.input);
      offlineStorage.remapDocId(op.applicationId, op.tempId, created.id);
      break;
    }
    case "UPDATE_DOC": {
      await documentApi.update(op.documentId, op.input);
      break;
    }
    case "DELETE_DOC": {
      await documentApi.delete(op.documentId);
      break;
    }
  }
}

/**
 * Replay all queued offline operations against the server.
 * Operations are processed sequentially. After each success the queue is
 * re-read from storage so that ID remaps (e.g. temp → real application ID)
 * are visible to subsequent operations before they run.
 */
export async function syncOfflineChanges(): Promise<SyncResult> {
  const result: SyncResult = {
    total: offlineStorage.getQueue().length,
    succeeded: 0,
    failed: 0,
    errors: [],
  };

  // Re-read the queue on every iteration so remapped IDs are always fresh.
  let queue = offlineStorage.getQueue();
  while (queue.length > 0) {
    const op = queue[0];
    try {
      await processOperation(op);
      offlineStorage.dequeue(op.id);
      result.succeeded++;
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      result.errors.push(`${op.kind} (${op.id}): ${msg}`);
      result.failed++;
      // Skip this operation and continue with the rest
      offlineStorage.dequeue(op.id);
    }
    queue = offlineStorage.getQueue();
  }

  return result;
}
