"use client";

import { useMutation } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Branch } from "@/lib/api/types";
import { usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { BranchPicker } from "./branch-picker";
import { useRefreshTeam } from "./team";

const t = messages.panel.team;

/** Invitar a alguien por email. El dueño elige el rol; el gerente invita solo profesionales. */
export function InviteSheet({ open, branches, onClose }: { open: boolean; branches: Branch[]; onClose: () => void }) {
  return (
    <Sheet open={open} onOpenChange={(next) => !next && onClose()} title={t.inviteTitle}>
      {open && <InviteForm branches={branches} onDone={onClose} />}
    </Sheet>
  );
}

function InviteForm({ branches, onDone }: { branches: Branch[]; onDone: () => void }) {
  const panel = usePanel();
  const refresh = useRefreshTeam();
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("BARBER");
  // Con una sola sucursal no hay nada que elegir.
  const [chosen, setChosen] = useState(branches.length === 1 ? [branches[0].id] : []);
  const invite = useMutation({
    mutationFn: () =>
      api(`/api/businesses/${panel.businessId}/invitations`, {
        method: "POST",
        body: { email, role, branchIds: chosen },
      }),
    onSuccess: () => {
      refresh();
      onDone();
    },
  });
  const emailError = invite.error instanceof ApiError ? invite.error.fieldErrors.email : undefined;

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        invite.mutate();
      }}
    >
      <p className="text-sm text-muted-foreground">{t.inviteHint}</p>
      <Field
        id="invite-email"
        label={t.email}
        type="email"
        inputMode="email"
        autoComplete="off"
        required
        value={email}
        error={emailError}
        onChange={(event) => setEmail(event.target.value)}
      />
      {panel.can("OWNER") && (
        <SelectField label={t.role} value={role} onChange={(event) => setRole(event.target.value)}>
          <option value="BARBER">{messages.panel.roles.BARBER}</option>
          <option value="MANAGER">{messages.panel.roles.MANAGER}</option>
        </SelectField>
      )}
      {branches.length > 1 && <BranchPicker branches={branches} value={chosen} onChange={setChosen} />}
      {invite.isError && !emailError && <Notice tone="error">{errorMessage(invite.error)}</Notice>}
      <Button type="submit" size="touch" disabled={invite.isPending || !email.trim() || chosen.length === 0}>
        {invite.isPending ? messages.panel.saving : t.invite}
      </Button>
    </form>
  );
}
