import type { MetadataRoute } from "next";

/** PWA manifest — ilovani telefon "home screen"ga o'rnatiladigan qiladi. */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "Opal — Fokus va Ekran Vaqti",
    short_name: "Opal",
    description:
      "Diqqatni boshqarish, ilovalarni bloklash, fokus sessiyalari va ekran vaqti statistikasi.",
    start_url: "/",
    display: "standalone",
    orientation: "portrait",
    background_color: "#05060f",
    theme_color: "#05060f",
    lang: "uz",
    categories: ["productivity", "lifestyle", "health"],
    icons: [
      {
        src: "/icons/icon-192.png",
        sizes: "192x192",
        type: "image/png",
        purpose: "any",
      },
      {
        src: "/icons/icon-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "any",
      },
      {
        src: "/icons/icon-512.png",
        sizes: "512x512",
        type: "image/png",
        purpose: "maskable",
      },
    ],
  };
}
