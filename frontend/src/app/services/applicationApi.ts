import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";
import { graphqlRequest } from "./graphqlClient";

export interface ApplicationPage {
  content: Application[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const applicationApi = {
  async getPage(page: number, size: number): Promise<ApplicationPage> {
    const query = `
      query($page: Int!, $size: Int!) {
        applications(page: $page, size: $size) {
          content { id type academicYear semester createdAt status }
          page
          size
          totalElements
          totalPages
        }
      }
    `;
    const data = await graphqlRequest<{ applications: ApplicationPage }>(query, { page, size });
    return data.applications;
  },

  async getAll(): Promise<Application[]> {
    const firstPage = await this.getPage(0, 1000);
    return firstPage.content;
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
