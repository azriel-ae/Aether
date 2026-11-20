import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';

export const runtime = 'nodejs';
export const maxDuration = 60;
const GEMINI_MODEL = 'gemini-3.8-flash';
const MAX_IMAGE_BASE64_CHARS = 4_000_000;
const schema = z.object({
  prompt: z.string().trim().min(1).max(8_000),
  imageBase64: z.string().min(1).max(MAX_IMAGE_BASE64_CHARS),
  mimeType: z.enum(['image/jpeg', 'image/png', 'image/webp']),
});

type InteractionResponse = {
  output_text?: string;
  steps?: Array<{ type?: string; content?: Array<{ type?: string; text?: string }> }>;
  error?: { message?: string };
};

export async function POST(request: NextRequest) {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) return NextResponse.json({ error: 'GEMINI_API_KEY belum diatur di Vercel.' }, { status: 503 });
  let raw: unknown;
  try { raw = await request.json(); }
  catch { return NextResponse.json({ error: 'Body JSON tidak valid.' }, { status: 400 }); }
  const parsed = schema.safeParse(raw);
  if (!parsed.success) return NextResponse.json({ error: 'Format permintaan scan tidak valid.', issues: parsed.error.issues }, { status: 400 });

  try {
    const upstream = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': apiKey, 'Api-Revision': '2026-05-20' },
      body: JSON.stringify({
        model: GEMINI_MODEL,
        input: [
          { type: 'text', text: `${parsed.data.prompt}\nAnalisis hanya konten yang pengguna berhak bagikan. Jangan menebak informasi pribadi yang tidak terlihat.` },
          { type: 'image', mime_type: parsed.data.mimeType, data: parsed.data.imageBase64 },
        ],
      }),
      signal: AbortSignal.timeout(50_000),
    });
    const result = await upstream.json() as InteractionResponse;
    if (!upstream.ok) return NextResponse.json({ error: 'Analisis Gemini gagal.', detail: result.error?.message ?? `Upstream HTTP ${upstream.status}` }, { status: 502 });
    const answer = result.output_text?.trim() || result.steps?.filter(s => s.type === 'model_output').flatMap(s => s.content ?? []).filter(c => c.type === 'text').map(c => c.text ?? '').join('\n').trim();
    if (!answer) return NextResponse.json({ error: 'Gemini tidak mengembalikan hasil analisis.' }, { status: 502 });
    return NextResponse.json({ answer, model: GEMINI_MODEL });
  } catch (error) {
    const detail = error instanceof Error ? error.message : 'Unknown network error';
    return NextResponse.json({ error: 'Tidak dapat menghubungi Gemini.', detail }, { status: 502 });
  }
}
