import { graphqlRequest } from "./graphqlClient";

export interface EnumValues {
  types: string[];
  statuses: string[];
  semesters: string[];
  academicYears: string[];
  documentTypes: string[];
}

export const enumApi = {
  async getAll(): Promise<EnumValues> {
    const query = `
      query {
        enums { types statuses semesters academicYears documentTypes }
      }
    `;
    const data = await graphqlRequest<{ enums: EnumValues }>(query);
    return data.enums;
  },
};
