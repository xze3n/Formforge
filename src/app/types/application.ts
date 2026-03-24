export type ApplicationType = "Merit" | "Social" | "Performance";
export type ApplicationStatus = "Draft" | "Pending Action" | "Approved";

export interface Application {
  id: number;
  type: ApplicationType;
  academicYear: string;
  semester: "I" | "II";
  createdAt: string;
  status: ApplicationStatus;
}

export interface CreateApplicationInput {
  type: ApplicationType;
  academicYear: string;
  semester: "I" | "II";
}

export interface UpdateApplicationInput {
  type?: ApplicationType;
  academicYear?: string;
  semester?: "I" | "II";
  status?: ApplicationStatus;
}
