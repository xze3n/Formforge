export interface EnumValues {
  types: string[];
  statuses: string[];
  semesters: string[];
  academicYears: string[];
}

export const enumApi = {
  async getAll(): Promise<EnumValues> {
    const response = await fetch("/api/enums");
    if (!response.ok) {
      throw new Error("Failed to load enum values");
    }
    return response.json();
  },
};
