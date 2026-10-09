"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/lib/api/client";
import { ApiError } from "@/lib/api/errors";
import type { Account } from "@/lib/api/types";

const ACCOUNT_KEY = ["account"] as const;

/** La cuenta con sesión iniciada, o null si nadie ingresó. */
export function useAccount() {
  return useQuery({
    queryKey: ACCOUNT_KEY,
    queryFn: async () => {
      try {
        return await api<Account>("/api/auth/me");
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          return null;
        }
        throw error;
      }
    },
    staleTime: 5 * 60_000,
  });
}

/** Después de ingresar o salir, la cuenta se vuelve a leer. */
export function useRefreshAccount() {
  const client = useQueryClient();
  return () => client.invalidateQueries({ queryKey: ACCOUNT_KEY });
}
