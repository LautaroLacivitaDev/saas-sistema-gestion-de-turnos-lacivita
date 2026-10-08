// Textos de la interfaz en español rioplatense.
// Todos los textos visibles salen de acá, para poder traducirlos más adelante.
export const messages = {
  app: {
    name: "Turnos",
    description:
      "Reservá tu turno en barberías y centros de estética, sin llamadas ni esperas.",
  },
  home: {
    title: "Tu próximo turno, en tres pasos",
    subtitle:
      "Buscá tu barbería o centro de estética, elegí el servicio y el profesional, y listo.",
    comingSoon: "Estamos preparando todo. Muy pronto vas a poder reservar desde acá.",
  },
} as const;

export type Messages = typeof messages;
