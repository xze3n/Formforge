import { graphqlRequest } from "./graphqlClient";

export interface EnumValues {
  types: string[];
  statuses: string[];
  semesters: string[];
  academicYears: string[];
}

export const enumApi = {
  async getAll(): Promise<EnumValues> {
    const query = `
      query {
        enums { types statuses semesters academicYears }
      }
    `;
    const data = await graphqlRequest<{ enums: EnumValues }>(query);
    return data.enums;
  },
};
