import { useState, useEffect } from "react";
import { enumApi, EnumValues } from "../services/enumApi";

const defaultEnums: EnumValues = {
  types: [],
  statuses: [],
  semesters: [],
  academicYears: [],
};

export const useEnums = () => {
  const [enums, setEnums] = useState<EnumValues>(defaultEnums);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    enumApi.getAll()
      .then(setEnums)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  return { enums, loading };
};
