"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Profile } from "@/lib/api/types";
import { panelKey, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { Offerings } from "./offerings";

const t = messages.panel.profile;

/** El perfil público y los servicios y precios de un profesional (por defecto, de quien ingresó). */
export function MyProfile({ barberId }: { barberId?: string }) {
  const panel = usePanel();
  const id = barberId ?? panel.userId;
  return (
    <div className="flex flex-col gap-6">
      {!barberId && <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>}
      <ProfileForm barberId={id} />
      <section aria-labelledby="mis-servicios" className="flex flex-col gap-3">
        <h2 id="mis-servicios" className="text-lg font-semibold">
          {t.myServices}
        </h2>
        <p className="text-sm text-muted-foreground">{t.myServicesHint}</p>
        <Offerings barberId={id} />
      </section>
    </div>
  );
}

function ProfileForm({ barberId }: { barberId: string }) {
  const panel = usePanel();
  const key = panelKey(panel.businessId, "profile", barberId);
  const profile = useQuery({
    queryKey: key,
    queryFn: () => api<Profile>(`/api/businesses/${panel.businessId}/barbers/${barberId}/profile`),
  });
  if (profile.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (profile.isError) {
    return <Notice tone="error">{errorMessage(profile.error)}</Notice>;
  }
  return <ProfileEditor barberId={barberId} profile={profile.data} queryKey={key} />;
}

function ProfileEditor({ barberId, profile, queryKey }: { barberId: string; profile: Profile; queryKey: unknown[] }) {
  const panel = usePanel();
  const client = useQueryClient();
  const [bio, setBio] = useState(profile.bio ?? "");
  const [specialties, setSpecialties] = useState(profile.specialties.join(", "));
  const [photoUrl, setPhotoUrl] = useState(profile.photoUrl ?? "");
  const save = useMutation({
    mutationFn: () =>
      api<Profile>(`/api/businesses/${panel.businessId}/barbers/${barberId}/profile`, {
        method: "PUT",
        body: {
          bio,
          photoUrl,
          specialties: specialties
            .split(",")
            .map((specialty) => specialty.trim())
            .filter(Boolean),
        },
      }),
    onSuccess: (updated) => {
      client.setQueryData(queryKey, updated);
      void client.invalidateQueries({ queryKey: ["professionals", panel.membership.slug] });
    },
  });
  const photoError =
    save.error instanceof ApiError && save.error.code === "invalid_photo_url" ? save.error.message : undefined;

  return (
    <form
      className="flex flex-col gap-4 rounded-xl border p-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <div className="flex flex-col gap-1">
        <h2 className="text-lg font-semibold">{t.publicProfile}</h2>
        <p className="text-sm text-muted-foreground">{t.publicHint}</p>
      </div>
      <label htmlFor="profile-bio" className="flex flex-col gap-1.5">
        <span className="text-sm font-medium">{t.bio}</span>
        <textarea
          id="profile-bio"
          rows={3}
          maxLength={500}
          value={bio}
          onChange={(event) => setBio(event.target.value)}
          className="rounded-lg border border-input bg-background px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
      </label>
      <Field
        id="profile-specialties"
        label={t.specialties}
        hint={t.specialtiesHint}
        autoComplete="off"
        value={specialties}
        onChange={(event) => setSpecialties(event.target.value)}
      />
      <Field
        id="profile-photo"
        label={t.photo}
        hint={t.photoHint}
        type="url"
        inputMode="url"
        autoComplete="off"
        value={photoUrl}
        error={photoError}
        onChange={(event) => setPhotoUrl(event.target.value)}
      />
      {save.isError && !photoError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      <Button type="submit" size="touch" className="self-start" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : messages.panel.save}
      </Button>
    </form>
  );
}
