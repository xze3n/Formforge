import { useEffect, useRef, useCallback } from "react";
import { Application } from "../types/application";

type WebSocketMessage =
  | { type: "APPLICATION_CREATED"; payload: Application }
  | { type: "GENERATOR_STOPPED" };

interface UseApplicationWebSocketOptions {
  onApplicationCreated: (app: Application) => void;
  onGeneratorStopped?: () => void;
}

export const useApplicationWebSocket = ({
  onApplicationCreated,
  onGeneratorStopped,
}: UseApplicationWebSocketOptions) => {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const cleanedUp = useRef(false);

  // Keep callbacks fresh without reconnecting
  const onCreatedRef = useRef(onApplicationCreated);
  onCreatedRef.current = onApplicationCreated;
  const onStoppedRef = useRef(onGeneratorStopped);
  onStoppedRef.current = onGeneratorStopped;

  const connect = useCallback(() => {
    if (cleanedUp.current) return;
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) return;

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const ws = new WebSocket(`${protocol}//${window.location.host}/ws/applications`);

    wsRef.current = ws;

    ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data);
        switch (message.type) {
          case "APPLICATION_CREATED":
            onCreatedRef.current(message.payload);
            break;
          case "GENERATOR_STOPPED":
            onStoppedRef.current?.();
            break;
        }
      } catch {
        // ignore malformed messages
      }
    };

    ws.onclose = () => {
      // Only handle if this is still the active WebSocket (avoids StrictMode race)
      if (wsRef.current !== ws) return;
      wsRef.current = null;
      if (!cleanedUp.current) {
        reconnectTimer.current = setTimeout(connect, 3000);
      }
    };

    ws.onerror = () => {
      ws.close();
    };
  }, []);

  useEffect(() => {
    cleanedUp.current = false;
    connect();
    return () => {
      cleanedUp.current = true;
      if (reconnectTimer.current) {
        clearTimeout(reconnectTimer.current);
        reconnectTimer.current = null;
      }
      if (wsRef.current) {
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [connect]);
};
