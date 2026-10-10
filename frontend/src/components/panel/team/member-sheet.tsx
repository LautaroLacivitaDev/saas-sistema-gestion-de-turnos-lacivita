"use client";

import { useMutation } from "@tanstack/react-query";
import { useState } from "react";

import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Branch, Member } from "@/lib/api/types";
import { usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { BranchPicker } from "./branch-picker";
import { useRefreshTeam } from "./team";

const t = messages.panel.team;

/** Editar a un miembro: rol (solo el dueño), sucursales y baja. */
export function MemberSheet({
  member,
  branches,
  onClose,
}: {
  member: Member | null;
  branches: Branch[];
  onClose: () => void;
}) {
  return (
    <Sheet open={member !== null} onOpenChange={(open) => !open && onClose()} title={member?.name ?? t.editMember}>
      {member && <MemberEditor key={member.userId} member={member} branches={branches} onDone={onClose} />}
    </Sheet>
  );
}

function MemberEditor({ member, branches, onDone }: { member: Member; branches: Branch[]; onDone: () => void }) {
  const panel = usePanel();
  const refresh = useRefreshTeam();
  const [role, setRole] = useState(member.role);
  const [chosen, setChosen] = useState(member.branchIds);
  const [confirming, setConfirming] = useState(false);
  const url = `/api/businesses/${panel.businessId}/members/${member.userId}`;
  const changeRole = useMutation({
    mutationFn: () => api(`${url}/role`, { method: "PUT", body: { role } }),
    onSuccess: refresh,
  });
  const assign = useMutation({
    mutationFn: () => api(`${url}/branches`, { method: "PUT", body: { branchIds: chosen } }),
    onSuccess: refresh,
  });
  const remove = useMutation({
    mutationFn: () => api(url, { method: "DELETE" }),
    onSuccess: () => {
      refresh();
      onDone();
    },
  });
  const error = changeRole.error ?? assign.error ?? remove.error;

  return (
    <div className="flex flex-col gap-6">
      <p className="text-sm text-muted-foreground">{member.email}</p>
      {panel.can("OWNER") && (
        <form
          className="flex flex-col gap-3"
          onSubmit={(event) => {
            event.preventDefault();
            changeRole.mutate();
          }}
        >
          <SelectField label={t.role} value={role} onChange={(event) => setRole(event.target.value)}>
            <option value="MANAGER">{messages.panel.roles.MANAGER}</option>
            <option value="BARBER">{messages.panel.roles.BARBER}</option>
          </SelectField>
          {changeRole.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
          <Button type="submit" size="touch" variant="outline" disabled={changeRole.isPending || role === member.role}>
            {t.saveRole}
          </Button>
        </form>
      )}
      <form
        className="flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          assign.mutate();
        }}
      >
        <BranchPicker branches={branches} value={chosen} onChange={setChosen} />
        {assign.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
        <Button type="submit" size="touch" variant="outline" disabled={assign.isPending || chosen.length === 0}>
          {t.saveBranches}
        </Button>
      </form>
      {error && <Notice tone="error">{errorMessage(error)}</Notice>}
      <div className="flex flex-col gap-3 border-t pt-4">
        {confirming ? (
          <>
            <Notice>{t.confirmRemove(member.name)}</Notice>
            <div className="flex gap-2">
              <Button size="touch" variant="destructive" disabled={remove.isPending} onClick={() => remove.mutate()}>
                {t.remove}
              </Button>
              <Button size="touch" variant="ghost" onClick={() => setConfirming(false)}>
                {messages.panel.cancel}
              </Button>
            </div>
          </>
        ) : (
          <Button size="touch" variant="destructive" onClick={() => setConfirming(true)}>
            {t.remove}
          </Button>
        )}
      </div>
    </div>
  );
}
