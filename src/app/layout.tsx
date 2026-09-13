import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Toaster } from "@/components/ui/sonner";
import { Providers } from "@/components/opal/providers";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Opal — Fokus va Ekran Vaqti",
  description:
    "Opal clone — diqqatni boshqarish, ilovalarni bloklash, fokus sessiyalari va ekran vaqti statistikasi.",
  keywords: ["Opal", "focus", "screen time", "fokus", "bloklash"],
  applicationName: "Opal",
  manifest: "/manifest.webmanifest",
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: "Opal",
  },
  icons: {
    icon: [
      { url: "/opal-icon.svg", type: "image/svg+xml" },
      { url: "/icons/icon-192.png", sizes: "192x192", type: "image/png" },
    ],
    apple: "/icons/apple-touch-icon.png",
  },
};

export const viewport: Viewport = {
  themeColor: "#05060f",
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="uz" suppressHydrationWarning>
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased bg-background text-foreground`}
      >
        <Providers>{children}</Providers>
        <Toaster
          position="top-center"
          theme="dark"
          gap={8}
          toastOptions={{
            classNames: {
              toast:
                '!bg-[#0c110d]/95 !text-white !border !border-white/12 !rounded-2xl !backdrop-blur-xl !shadow-[0_14px_44px_rgba(0,0,0,0.65),inset_0_1px_0_rgba(255,255,255,0.07)]',
              title: '!text-[13px] !font-bold !text-white',
              description: '!text-[11.5px] !text-white/55',
              success: '!border-[#86efac]/30',
              error: '!border-rose-400/35',
              warning: '!border-amber-300/35',
              info: '!border-white/12',
              icon: '!text-[#9fe8b5]',
            },
          }}
        />
      </body>
    </html>
  );
}
