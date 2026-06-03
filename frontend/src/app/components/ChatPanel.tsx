import { useState, useRef, useEffect } from "react";
import { MessageCircle, X, Send } from "lucide-react";
import { useChatWebSocket, type ChatMessageData } from "../hooks/useChatWebSocket";
import type { AuthUser } from "../services/authService";

interface ChatPanelProps {
  user: AuthUser;
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  } catch {
    return "";
  }
}

function MessageBubble({
  msg,
  isMine,
}: {
  msg: ChatMessageData;
  isMine: boolean;
}) {
  return (
    <div className={`flex flex-col ${isMine ? "items-end" : "items-start"} mb-2`}>
      {!isMine && (
        <span className="text-xs text-gray-500 mb-0.5 px-1">{msg.username}</span>
      )}
      <div
        className={`max-w-[80%] px-3 py-2 rounded-2xl text-sm break-words ${
          isMine
            ? "bg-purple-600 text-white rounded-br-sm"
            : "bg-gray-100 text-gray-900 rounded-bl-sm"
        }`}
      >
        {msg.text}
      </div>
      <span className="text-[10px] text-gray-400 mt-0.5 px-1">
        {formatTime(msg.timestamp)}
      </span>
    </div>
  );
}

const LAST_READ_KEY = "chat_last_read_ts";

function getLastReadTs(): string {
  return sessionStorage.getItem(LAST_READ_KEY) ?? "";
}

function saveLastReadTs(ts: string): void {
  sessionStorage.setItem(LAST_READ_KEY, ts);
}

export function ChatPanel({ user }: ChatPanelProps) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState("");
  const [lastReadTs, setLastReadTs] = useState<string>(getLastReadTs);
  const bottomRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const { messages, connected, sendMessage } = useChatWebSocket({
    userId: user.id,
    username: user.username,
  });

  const unread = messages.filter((m) => m.timestamp > lastReadTs).length;

  // Mark all messages as read whenever the panel is open
  useEffect(() => {
    if (open && messages.length > 0) {
      const latest = messages[messages.length - 1].timestamp;
      setLastReadTs(latest);
      saveLastReadTs(latest);
    }
  }, [open, messages]);

  // Scroll to bottom when messages change or panel opens
  useEffect(() => {
    if (open) {
      bottomRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, open]);

  // Focus input when panel opens
  useEffect(() => {
    if (open) inputRef.current?.focus();
  }, [open]);

  const handleSend = () => {
    const text = input.trim();
    if (!text || !connected) return;
    sendMessage(text);
    setInput("");
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <>
      {/* Floating toggle button */}
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="relative flex items-center justify-center w-14 h-14 rounded-full bg-purple-600 text-white shadow-lg hover:bg-purple-700 transition-colors"
        aria-label={open ? "Close chat" : "Open chat"}
      >
        {open ? <X className="w-6 h-6" /> : <MessageCircle className="w-6 h-6" />}
        {!open && unread > 0 && (
          <span className="absolute -top-1 -right-1 flex items-center justify-center w-5 h-5 rounded-full bg-red-500 text-[10px] font-bold">
            {unread > 99 ? "99+" : unread}
          </span>
        )}
      </button>

      {/* Chat panel */}
      {open && (
        <div className="fixed bottom-24 right-5 z-50 w-80 sm:w-96 flex flex-col bg-white border border-gray-200 rounded-2xl shadow-2xl overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between px-4 py-3 bg-purple-600 text-white">
            <div className="flex items-center gap-2">
              <MessageCircle className="w-5 h-5" />
              <span className="font-semibold text-sm">Live Chat</span>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="hover:text-purple-200 transition-colors"
                aria-label="Close chat"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          </div>

          {/* Messages */}
          <div className="h-72 overflow-y-auto p-3 space-y-0.5 bg-white">
            {messages.length === 0 && (
              <p className="text-center text-gray-400 text-sm mt-8">
                No messages yet. Say hello!
              </p>
            )}
            {messages.map((msg) => (
              <MessageBubble
                key={msg.id}
                msg={msg}
                isMine={msg.userId === user.id}
              />
            ))}
            <div ref={bottomRef} />
          </div>

          {/* Input */}
          <div className="flex items-center gap-2 px-3 py-2 border-t border-gray-100 bg-gray-50">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={connected ? "Type a message…" : "Connecting…"}
              disabled={!connected}
              className="flex-1 text-sm bg-white border border-gray-200 rounded-full px-4 py-2 outline-none focus:ring-2 focus:ring-purple-400 disabled:opacity-50"
            />
            <button
              type="button"
              onClick={handleSend}
              disabled={!connected || !input.trim()}
              className="flex items-center justify-center w-9 h-9 rounded-full bg-purple-600 text-white hover:bg-purple-700 disabled:opacity-40 disabled:cursor-not-allowed transition-colors shrink-0"
              aria-label="Send message"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </>
  );
}
