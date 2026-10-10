"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { PlusIcon } from "lucide-react";
import { useState } from "react";

import { Avatar } from "@/components/avatar";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Invitation, Member, Page } from "@/lib/api/types";
import { dateTime } from "@/lib/format";
import { panelKey, useManagedBranches, useMembers, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { InviteSheet } from "./invite-sheet";
import { MemberSheet } from "./member-sheet";

const t = messages.panel.team;

/** Si quien ingresó puede editar o dar de baja a ese miembro. */
export function canManage(member: Member, actorIsOwner: boolean, managed: Set<string>) {
  if (member.role === "OWNER") {
    return false;
  }
  if (actorIsOwner) {
    return true;
  }
  return member.role === "BARBER" && member.branchIds.every((id) => managed.has(id));
}

export function useRefreshTeam() {
  const panel = usePanel();
  const client = useQueryClient();
  return () => {
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "members") });
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "invitations") });
    void client.invalidateQueries({ queryKey: ["professionals", panel.membership.slug] });
  };
}

/** Miembros del equipo y las invitaciones pendientes. Dueño y gerentes. */
export function Team() {
  const panel = usePanel();
  const members = useMembers(panel.businessId);
  const managed = useManagedBranches();
  const [editing, setEditing] = useState<Member | null>(null);
  const [inviting, setInviting] = useState(false);
  const managedIds = new Set(managed.map((branch) => branch.id));
  const branchNames = new Map(panel.branches.map((branch) => [branch.id, branch.name]));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        <Button size="touch" onClick={() => setInviting(true)} disabled={managed.length === 0}>
          <PlusIcon aria-hidden />
          {t.invite}
        </Button>
      </div>
      <section aria-labelledby="miembros" className="flex flex-col gap-3">
        <h2 id="miembros" className="text-lg font-semibold">
          {t.members}
        </h2>
        {members.isError && <Notice tone="error">{errorMessage(members.error)}</Notice>}
        {members.isPending ? (
          <p className="text-muted-foreground">{messages.panel.loading}</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {(members.data ?? []).map((member) => {
              const manageable = canManage(member, panel.can("OWNER"), managedIds);
              return (
                <li key={member.userId}>
                  <button
                    type="button"
                    disabled={!manageable}
                    onClick={() => setEditing(member)}
                    className="flex min-h-14 w-full items-center gap-3 rounded-xl border px-4 py-3 text-left enabled:hover:bg-muted/50"
                  >
                    <Avatar name={member.name} photoUrl={null} size={40} />
                    <span className="flex min-w-0 flex-1 flex-col">
                      <span className="truncate font-medium">
                        {member.name}
                        {member.userId === panel.userId && ` (${t.you})`}
                      </span>
                      <span className="truncate text-sm text-muted-foreground">{member.email}</span>
                      <span className="text-sm text-muted-foreground">
                        {member.branchIds.map((id) => branchNames.get(id) ?? "").join(", ") || messages.panel.allBranches}
                      </span>
                    </span>
                    <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs">
                      {messages.panel.roles[member.role] ?? member.role}
                    </span>
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </section>
      <Invitations />
      <MemberSheet member={editing} branches={managed} onClose={() => setEditing(null)} />
      <InviteSheet open={inviting} branches={managed} onClose={() => setInviting(false)} />
    </div>
  );
}

function Invitations() {
  const panel = usePanel();
  const refresh = useRefreshTeam();
  const timeZone = panel.branches[0]?.timeZone ?? "America/Argentina/Buenos_Aires";
  const invitations = useQuery({
    queryKey: panelKey(panel.businessId, "invitations"),
    queryFn: async () =>
      (await api<Page<Invitation>>(`/api/businesses/${panel.businessId}/invitations?size=100`)).items,
  });
  const revoke = useMutation({
    mutationFn: (id: string) => api(`/api/businesses/${panel.businessId}/invitations/${id}`, { method: "DELETE" }),
    onSuccess: refresh,
  });
  return (
    <section aria-labelledby="invitaciones" className="flex flex-col gap-3">
      <h2 id="invitaciones" className="text-lg font-semibold">
        {t.invitations}
      </h2>
      {(invitations.isError || revoke.isError) && (
        <Notice tone="error">{errorMessage(invitations.error ?? revoke.error)}</Notice>
      )}
      {invitations.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : (invitations.data ?? []).length === 0 ? (
        <p className="text-muted-foreground">{t.noInvitations}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {(invitations.data ?? []).map((invitation) => (
            <li key={invitation.id} className="flex items-center gap-3 rounded-xl border px-4 py-3">
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="truncate font-medium">{invitation.email}</span>
                <span className="text-sm text-muted-foreground">
                  {messages.panel.roles[invitation.role] ?? invitation.role} ·{" "}
                  {t.pendingUntil(dateTime(invitation.expiresAt, timeZone))}
                </span>
              </span>
              <Button
                size="touch"
                variant="outline"
                className="shrink-0"
                disabled={revoke.isPending}
                onClick={() => revoke.mutate(invitation.id)}
              >
                {t.revoke}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
