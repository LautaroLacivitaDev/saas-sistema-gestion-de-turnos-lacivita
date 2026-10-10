import { MyProfile } from "@/components/panel/profile/my-profile";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.profile.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function ProfilePage() {
  return <MyProfile />;
}
