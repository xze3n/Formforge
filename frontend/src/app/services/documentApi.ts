import { Document, CreateDocumentInput, UpdateDocumentInput } from "../types/document";
import { graphqlRequest } from "./graphqlClient";

const DOCUMENT_FIELDS = "id applicationId name type description dateAdded verified notes";

export const documentApi = {
  async getByApplicationId(applicationId: number): Promise<Document[]> {
    const query = `
      query($applicationId: ID!) {
        documents(applicationId: $applicationId) { ${DOCUMENT_FIELDS} }
      }
    `;
    const data = await graphqlRequest<{ documents: Document[] }>(query, { applicationId });
    return data.documents;
  },

  async getById(id: number): Promise<Document> {
    const query = `
      query($id: ID!) {
        document(id: $id) { ${DOCUMENT_FIELDS} }
      }
    `;
    const data = await graphqlRequest<{ document: Document }>(query, { id });
    return data.document;
  },

  async create(applicationId: number, input: CreateDocumentInput): Promise<Document> {
    const query = `
      mutation($applicationId: ID!, $input: CreateDocumentInput!) {
        createDocument(applicationId: $applicationId, input: $input) { ${DOCUMENT_FIELDS} }
      }
    `;
    const data = await graphqlRequest<{ createDocument: Document }>(query, { applicationId, input });
    return data.createDocument;
  },

  async update(id: number, input: UpdateDocumentInput): Promise<Document> {
    const query = `
      mutation($id: ID!, $input: UpdateDocumentInput!) {
        updateDocument(id: $id, input: $input) { ${DOCUMENT_FIELDS} }
      }
    `;
    const data = await graphqlRequest<{ updateDocument: Document }>(query, { id, input });
    return data.updateDocument;
  },

  async delete(id: number): Promise<void> {
    const query = `
      mutation($id: ID!) {
        deleteDocument(id: $id)
      }
    `;
    await graphqlRequest<{ deleteDocument: boolean }>(query, { id });
  },
};
