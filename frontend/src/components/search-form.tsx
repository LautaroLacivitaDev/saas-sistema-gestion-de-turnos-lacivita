import Form from "next/form";
import { SearchIcon } from "lucide-react";

import { Button } from "@/components/ui/button";
import { messages } from "@/messages/es-AR";

/** Buscador de negocios. Funciona también sin JavaScript (es un formulario GET a /buscar). */
export function SearchForm({ defaultValue = "" }: { defaultValue?: string }) {
  const t = messages.search;
  return (
    <Form action="/buscar" role="search" className="flex w-full gap-2">
      <label htmlFor="q" className="sr-only">
        {t.label}
      </label>
      <input
        id="q"
        name="q"
        type="search"
        inputMode="search"
        enterKeyHint="search"
        autoComplete="off"
        defaultValue={defaultValue}
        placeholder={t.placeholder}
        className="h-11 min-w-0 flex-1 rounded-lg border border-input bg-background px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
      />
      <Button type="submit" size="touch" aria-label={t.submit}>
        <SearchIcon aria-hidden />
        <span className="hidden sm:inline">{t.submit}</span>
      </Button>
    </Form>
  );
}
