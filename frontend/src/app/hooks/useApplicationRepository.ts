import { useState, useReducer, useCallback, useEffect } from "react";
import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";
import { applicationApi } from "../services/applicationApi";

type Action =
  | { type: "SET_APPLICATIONS"; payload: Application[] }
  | { type: "ADD_APPLICATION"; payload: Application }
  | { type: "UPDATE_APPLICATION"; payload: Application }
  | { type: "DELETE_APPLICATION"; payload: number };

const appsReducer = (state: Application[], action: Action): Application[] => {
  switch (action.type) {
    case "SET_APPLICATIONS":
      return action.payload;
    case "ADD_APPLICATION":
      return [...state, action.payload];
    case "UPDATE_APPLICATION":
      return state.map(app => 
        app.id === action.payload.id ? action.payload : app
      );
    case "DELETE_APPLICATION":
      return state.filter(app => app.id !== action.payload);
    default:
      return state;
  }
};

/**
 * Custom hook to manage applications via the backend API
 */
export const useApplicationRepository = () => {
  const [applications, dispatch] = useReducer(appsReducer, []);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

  // Fetch all applications on mount
  useEffect(() => {
    applicationApi.getAll()
      .then(apps => dispatch({ type: "SET_APPLICATIONS", payload: apps }))
      .catch(err => setError(err instanceof Error ? err.message : "Failed to load applications"))
      .finally(() => setLoading(false));
  }, []);

  const getAll = useCallback((): Application[] => {
    return applications;
  }, [applications]);

  const getById = useCallback((id: number): Application | undefined => {
    return applications.find(app => app.id === id);
  }, [applications]);

  const add = useCallback(async (input: CreateApplicationInput): Promise<Application> => {
    try {
      clearError();
      const newApplication = await applicationApi.create(input);
      dispatch({ type: "ADD_APPLICATION", payload: newApplication });
      return newApplication;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to create application";
      setError(message);
      throw err;
    }
  }, [clearError]);

  const update = useCallback(
    async (id: number, input: UpdateApplicationInput): Promise<Application> => {
      try {
        clearError();
        const updated = await applicationApi.update(id, input);
        dispatch({ type: "UPDATE_APPLICATION", payload: updated });
        return updated;
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to update application";
        setError(message);
        throw err;
      }
    },
    [clearError]
  );

  const remove = useCallback(async (id: number): Promise<boolean> => {
    try {
      clearError();
      await applicationApi.delete(id);
      dispatch({ type: "DELETE_APPLICATION", payload: id });
      return true;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to delete application";
      setError(message);
      throw err;
    }
  }, [clearError]);

  return {
    applications,
    loading,
    error,
    clearError,
    getAll,
    getById,
    add,
    update,
    remove,
    delete: remove,
  };
};
