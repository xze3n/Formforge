import { useEffect, useRef, useCallback, useState } from "react";

export interface ChatMessageData {
  id: number;
  userId: number;
  username: string;
  text: string;
  timestamp: string; // ISO string from backend
}

type IncomingMessage =
  | { type: "HISTORY"; messages: ChatMessageData[] }
  | { type: "CHAT_MESSAGE"; message: ChatMessageData };

interface UseChatWebSocketOptions {
  userId: number;
  username: string;
}

export function useChatWebSocket({ userId, username }: UseChatWebSocketOptions) {
  const [messages, setMessages] = useState<ChatMessageData[]>([]);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const cleanedUp = useRef(false);

  const connect = useCallback(() => {
    if (cleanedUp.current) return;
    if (wsRef.current?.readyState === WebSocket.OPEN) return;

    const apiUrl = import.meta.env.VITE_API_URL as string | undefined;
    const wsBase = apiUrl
      ? apiUrl.replace(/^https/, "wss").replace(/^http/, "ws")
      : `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}`;
    const ws = new WebSocket(`${wsBase}/ws/chat`);
    wsRef.current = ws;

    ws.onopen = () => setConnected(true);

    ws.onmessage = (event) => {
      try {
        const msg: IncomingMessage = JSON.parse(event.data as string);
        if (msg.type === "HISTORY") {
          setMessages(msg.messages);
        } else if (msg.type === "CHAT_MESSAGE") {
          setMessages((prev) => [...prev, msg.message]);
        }
      } catch {
        // ignore malformed frames
      }
    };

    ws.onclose = () => {
      setConnected(false);
      if (!cleanedUp.current) {
        reconnectTimer.current = setTimeout(connect, 3000);
      }
    };

    ws.onerror = () => ws.close();
  }, []);

  useEffect(() => {
    cleanedUp.current = false;
    connect();
    return () => {
      cleanedUp.current = true;
      if (reconnectTimer.current) clearTimeout(reconnectTimer.current);
      wsRef.current?.close();
    };
  }, [connect]);

  const sendMessage = useCallback(
    (text: string) => {
      if (wsRef.current?.readyState !== WebSocket.OPEN) return;
      wsRef.current.send(
        JSON.stringify({ type: "CHAT_MESSAGE", userId, username, text })
      );
    },
    [userId, username]
  );

  return { messages, connected, sendMessage };
}
