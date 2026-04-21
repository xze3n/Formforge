export type DocumentType =
  | "Student Enrollment Certificate"
  | "Social Assessment Report"
  | "Income Certificate"
  | "Tax Certificate"
  | "ID Copy"
  | "Birth Certificate"
  | "Medical Certificate"
  | "Self-Declaration"
  | "Pension Slip"
  | "Death Certificate";

export interface Document {
  id: number;
  applicationId: number;
  name: string;
  type: DocumentType;
  description: string | null;
  dateAdded: string;
  verified: boolean;
  notes: string | null;
}

export interface CreateDocumentInput {
  name: string;
  type: DocumentType;
  description?: string;
  notes?: string;
}

export interface UpdateDocumentInput {
  name?: string;
  type?: DocumentType;
  description?: string;
  notes?: string;
  verified?: boolean;
}
