import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";

const BASE_URL = "/api/applications";

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const message = body.error || Object.values(body).join(", ") || response.statusText;
    throw new Error(message);
  }
  return response.json();
}

export const applicationApi = {
  async getAll(): Promise<Application[]> {
    const response = await fetch(`${BASE_URL}?page=0&size=1000`);
    const page = await handleResponse<PageResponse<Application>>(response);
    return page.content;
  },

  async getById(id: number): Promise<Application> {
    const response = await fetch(`${BASE_URL}/${id}`);
    return handleResponse<Application>(response);
  },

  async create(input: CreateApplicationInput): Promise<Application> {
    const response = await fetch(BASE_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
    return handleResponse<Application>(response);
  },

  async update(id: number, input: UpdateApplicationInput): Promise<Application> {
    const response = await fetch(`${BASE_URL}/${id}`, {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
    });
    return handleResponse<Application>(response);
  },

  async delete(id: number): Promise<void> {
    const response = await fetch(`${BASE_URL}/${id}`, {
      method: "DELETE",
    });
    if (!response.ok) {
      const body = await response.json().catch(() => ({}));
      throw new Error(body.error || "Failed to delete application");
    }
  },
};
