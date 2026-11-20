import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';

export const runtime = 'nodejs';
export const maxDuration = 60;
const GEMINI_MODEL = 'gemini-3.8-flash';
const schema = z.object({
  messages: z.array(z.object({ role: z.enum(['user', 'model']), text: z.string().min(1).max(8_000) })).min(1).max(30),
});

type InteractionResponse = {
  output_text?: string;
  steps?: Array<{ type?: string; content?: Array<{ type?: string; text?: string }> }>;
  error?: { message?: string };
};

export async function POST(request: NextRequest) {
  const key = process.env.GEMINI_API_KEY;
  if (!key) return NextResponse.json({ error: 'GEMINI_API_KEY belum diatur di Vercel.' }, { status: 503 });
  let raw: unknown;
  try { raw = await request.json(); } catch { return NextResponse.json({ error: 'Body JSON tidak valid.' }, { status: 400 }); }
  const parsed = schema.safeParse(raw);
  if (!parsed.success) return NextResponse.json({ error: 'Format pesan tidak valid.', issues: parsed.error.issues }, { status: 400 });

  const conversation = parsed.data.messages.map((m) => `${m.role === 'user' ? 'Pengguna' : 'Aether'}: ${m.text}`).join('\n\n');
  try {
    const upstream = await fetch('https://generativelanguage.googleapis.com/v1beta/interactions', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'x-goog-api-key': key, 'Api-Revision': '2026-05-20' },
      body: JSON.stringify({ model: GEMINI_MODEL, input: conversation }),
      signal: AbortSignal.timeout(50_000),
    });
    const result = await upstream.json() as InteractionResponse;
    if (!upstream.ok) return NextResponse.json({ error: 'Permintaan Gemini gagal.', detail: result.error?.message ?? `Upstream HTTP ${upstream.status}` }, { status: 502 });
    const answer = result.output_text?.trim() || result.steps?.filter(s => s.type === 'model_output').flatMap(s => s.content ?? []).filter(c => c.type === 'text').map(c => c.text ?? '').join('\n').trim();
    if (!answer) return NextResponse.json({ error: 'Gemini tidak mengembalikan teks jawaban.' }, { status: 502 });
    return NextResponse.json({ answer, model: GEMINI_MODEL });
  } catch (error) {
    const detail = error instanceof Error ? error.message : 'Unknown network error';
    return NextResponse.json({ error: 'Tidak dapat menghubungi Gemini.', detail }, { status: 502 });
  }
}
