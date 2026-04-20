import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";
import { graphqlRequest } from "./graphqlClient";

export const applicationApi = {
  async getAll(): Promise<Application[]> {
    const query = `
      query {
        applications(page: 0, size: 1000) {
          content { id type academicYear semester createdAt status }
        }
      }
    `;
    const data = await graphqlRequest<{ applications: { content: Application[] } }>(query);
    return data.applications.content;
  },

  async getById(id: number): Promise<Application> {
    const query = `
      query($id: ID!) {
        application(id: $id) { id type academicYear semester createdAt status }
      }
    `;
    const data = await graphqlRequest<{ application: Application }>(query, { id });
    return data.application;
  },

  async create(input: CreateApplicationInput): Promise<Application> {
    const query = `
      mutation($input: CreateApplicationInput!) {
        createApplication(input: $input) { id type academicYear semester createdAt status }
      }
    `;
    const data = await graphqlRequest<{ createApplication: Application }>(query, { input });
    return data.createApplication;
  },

  async update(id: number, input: UpdateApplicationInput): Promise<Application> {
    const query = `
      mutation($id: ID!, $input: UpdateApplicationInput!) {
        updateApplication(id: $id, input: $input) { id type academicYear semester createdAt status }
      }
    `;
    const data = await graphqlRequest<{ updateApplication: Application }>(query, { id, input });
    return data.updateApplication;
  },

  async delete(id: number): Promise<void> {
    const query = `
      mutation($id: ID!) {
        deleteApplication(id: $id)
      }
    `;
    await graphqlRequest<{ deleteApplication: boolean }>(query, { id });
  },
};
