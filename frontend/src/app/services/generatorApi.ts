import { graphqlRequest } from "./graphqlClient";

export const generatorApi = {
  async start(): Promise<{ running: boolean }> {
    const query = `
      mutation {
        startGenerator { running }
      }
    `;
    const data = await graphqlRequest<{ startGenerator: { running: boolean } }>(query);
    return data.startGenerator;
  },

  async stop(): Promise<{ running: boolean }> {
    const query = `
      mutation {
        stopGenerator { running }
      }
    `;
    const data = await graphqlRequest<{ stopGenerator: { running: boolean } }>(query);
    return data.stopGenerator;
  },

  async status(): Promise<{ running: boolean }> {
    const query = `
      query {
        generatorStatus { running }
      }
    `;
    const data = await graphqlRequest<{ generatorStatus: { running: boolean } }>(query);
    return data.generatorStatus;
  },
};
