import { useState, useEffect } from "react";
import { enumApi, EnumValues } from "../services/enumApi";
import { offlineStorage } from "../services/offlineStorage";

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
      .then((data) => {
        setEnums(data);
        offlineStorage.saveEnums(data);
      })
      .catch(() => {
        // Server unreachable – use cached enums
        const cached = offlineStorage.getEnums();
        if (cached) setEnums(cached);
      })
      .finally(() => setLoading(false));
  }, []);

  return { enums, loading };
};
