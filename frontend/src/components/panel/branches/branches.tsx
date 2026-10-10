"use client";

import { ChevronRightIcon, PlusIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { useManagedBranches, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { BranchSheet } from "./branch-sheet";

const t = messages.panel.branches;

export function Branches() {
  const panel = usePanel();
  const branches = useManagedBranches();
  const [creating, setCreating] = useState(false);
  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        {panel.can("OWNER") && (
          <Button size="touch" onClick={() => setCreating(true)}>
            <PlusIcon aria-hidden />
            {t.newBranch}
          </Button>
        )}
      </div>
      <ul className="divide-y rounded-xl border">
        {branches.map((branch) => (
          <li key={branch.id}>
            <Link
              href={`/panel/${panel.businessId}/sucursales/${branch.id}`}
              className="flex min-h-14 items-center justify-between gap-3 px-4 py-3 hover:bg-muted/50"
            >
              <span className="flex min-w-0 flex-col">
                <span className="font-medium">{branch.name}</span>
                <span className="truncate text-sm text-muted-foreground">
                  {[branch.street, branch.neighborhood, branch.city].filter(Boolean).join(", ")}
                </span>
              </span>
              <ChevronRightIcon aria-hidden className="size-5 shrink-0 text-muted-foreground" />
            </Link>
          </li>
        ))}
      </ul>
      <BranchSheet branch={creating ? "new" : null} onClose={() => setCreating(false)} />
    </div>
  );
}
