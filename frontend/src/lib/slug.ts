/** Lo que el servidor guardaría como link: minúsculas y guiones en lugar de espacios. */
export function normalizeSlug(value: string): string {
  return value.toLowerCase().replace(/\s+/g, "-");
}

/** Un link a partir del nombre del negocio: "Barbería El Tano" → "barberia-el-tano". */
export function suggestSlug(name: string): string {
  return normalizeSlug(
    name
      .normalize("NFD")
      .replace(/\p{Diacritic}/gu, "")
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, "")
      .trim(),
  )
    .replace(/-+/g, "-")
    .slice(0, 50);
}
