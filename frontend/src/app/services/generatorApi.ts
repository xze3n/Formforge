export const generatorApi = {
  async start(): Promise<{ running: boolean; started: boolean }> {
    const response = await fetch("/api/generator/start", { method: "POST" });
    if (!response.ok) throw new Error("Failed to start generator");
    return response.json();
  },

  async stop(): Promise<{ running: boolean; stopped: boolean }> {
    const response = await fetch("/api/generator/stop", { method: "POST" });
    if (!response.ok) throw new Error("Failed to stop generator");
    return response.json();
  },

  async status(): Promise<{ running: boolean }> {
    const response = await fetch("/api/generator/status");
    if (!response.ok) throw new Error("Failed to get generator status");
    return response.json();
  },
};
