import { useState, useReducer, useCallback } from "react";
import { Application, CreateApplicationInput, UpdateApplicationInput } from "../types/application";
import { applicationRepository } from "../services/applicationRepository";

type Action =
  | { type: "SET_APPLICATIONS"; payload: Application[] }
  | { type: "ADD_APPLICATION"; payload: Application }
  | { type: "UPDATE_APPLICATION"; payload: Application }
  | { type: "DELETE_APPLICATION"; payload: number }
  | { type: "RESET" };

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
    case "RESET":
      applicationRepository.reset();
      return applicationRepository.getAll();
    default:
      return state;
  }
};

/**
 * Custom hook to manage applications using the application repository
 */
export const useApplicationRepository = () => {
  const [applications, dispatch] = useReducer(
    appsReducer,
    [],
    () => applicationRepository.getAll()
  );
  const [error, setError] = useState<string | null>(null);

  const clearError = useCallback(() => setError(null), []);

  const getAll = useCallback((): Application[] => {
    return applications;
  }, [applications]);

  const getById = useCallback((id: number): Application | undefined => {
    return applicationRepository.getById(id);
  }, []);

  const add = useCallback((input: CreateApplicationInput): Application => {
    try {
      clearError();
      const newApplication = applicationRepository.create(input);
      dispatch({ type: "ADD_APPLICATION", payload: newApplication });
      return newApplication;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to create application";
      setError(message);
      throw err;
    }
  }, [clearError]);

  const update = useCallback(
    (id: number, input: UpdateApplicationInput): Application | undefined => {
      try {
        clearError();
        const updated = applicationRepository.update(id, input);
        if (updated) {
          dispatch({ type: "UPDATE_APPLICATION", payload: updated });
        }
        return updated;
      } catch (err) {
        const message = err instanceof Error ? err.message : "Failed to update application";
        setError(message);
        throw err;
      }
    },
    [clearError]
  );

  const remove = useCallback((id: number): boolean => {
    try {
      clearError();
      const deleted = applicationRepository.delete(id);
      if (deleted) {
        dispatch({ type: "DELETE_APPLICATION", payload: id });
      }
      return deleted;
    } catch (err) {
      const message = err instanceof Error ? err.message : "Failed to delete application";
      setError(message);
      throw err;
    }
  }, [clearError]);

  const reset = useCallback(() => {
    dispatch({ type: "RESET" });
  }, []);

  return {
    applications,
    error,
    clearError,
    getAll,
    getById,
    add,
    update,
    remove,
    delete: remove, // Alias for convenience
    reset,
  };
};
