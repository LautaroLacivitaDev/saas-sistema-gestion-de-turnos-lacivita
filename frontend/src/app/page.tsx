import { messages } from "@/messages/es-AR";

export default function Home() {
  const t = messages.home;

  return (
    <main className="flex flex-1 flex-col items-center justify-center px-4 py-16">
      <div className="flex w-full max-w-xl flex-col gap-4 text-center">
        <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">
          {t.title}
        </h1>
        <p className="text-lg text-muted-foreground">{t.subtitle}</p>
        <p className="text-sm text-muted-foreground">{t.comingSoon}</p>
      </div>
    </main>
  );
}
