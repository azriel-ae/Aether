import { NextResponse } from 'next/server';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET() {
  const hasGeminiKey = Boolean(process.env.GEMINI_API_KEY);
  return NextResponse.json({
    status: hasGeminiKey ? 'configured' : 'needs_configuration',
    service: 'aether-api',
    model: 'gemini-3.8-flash',
    checks: {
      geminiApiKey: hasGeminiKey ? 'configured' : 'missing',
    },
  }, { status: 200, headers: { 'Cache-Control': 'no-store' } });
}
