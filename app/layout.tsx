import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Aether — Gemini Workspace',
  description: 'Chat with Gemini and analyze images in Aether.',
  applicationName: 'Aether',
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="id">
      <body>{children}</body>
    </html>
  );
}
