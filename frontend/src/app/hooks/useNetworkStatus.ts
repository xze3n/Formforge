import { useState, useEffect, useCallback } from "react";
import { networkService } from "../services/networkService";
import { offlineStorage } from "../services/offlineStorage";

export const useNetworkStatus = () => {
  const [online, setOnline] = useState(networkService.online);
  const [pendingCount, setPendingCount] = useState(offlineStorage.pendingCount);

  useEffect(() => {
    const unsub = networkService.subscribe((value) => {
      setOnline(value);
      setPendingCount(offlineStorage.pendingCount);
    });
    return unsub;
  }, []);

  const refreshPendingCount = useCallback(() => {
    setPendingCount(offlineStorage.pendingCount);
  }, []);

  return { online, pendingCount, refreshPendingCount };
};
