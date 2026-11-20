"use client";

import { FormEvent, useEffect, useRef, useState } from "react";

type Message = { role: "user" | "model"; text: string };
type Tab = "chat" | "scan";

export default function Home() {
  const [tab, setTab] = useState<Tab>("chat");
  const [messages, setMessages] = useState<Message[]>([]);
  const [draft, setDraft] = useState("");
  const [sending, setSending] = useState(false);
  const [scanPrompt, setScanPrompt] = useState("Jelaskan isi gambar ini dengan jelas dan terstruktur.");
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState("");
  const [scanAnswer, setScanAnswer] = useState("");
  const [scanBusy, setScanBusy] = useState(false);
  const [error, setError] = useState("");
  const [service, setService] = useState<"checking" | "ready" | "missing" | "offline">("checking");
  const [serviceMessage, setServiceMessage] = useState("Memeriksa koneksi");
  const fileInput = useRef<HTMLInputElement>(null);
  const chatEnd = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let alive = true;
    fetch("/api/health", { cache: "no-store" })
      .then(async (r) => {
        const data = await r.json();
        if (!alive) return;
        if (data.status === "configured") {
          setService("ready");
          setServiceMessage("API key terpasang · siap dites");
        } else {
          setService("missing");
          setServiceMessage("API key belum dikonfigurasi");
        }
      })
      .catch(() => {
        if (!alive) return;
        setService("offline");
        setServiceMessage("Backend tidak dapat dihubungi");
      });
    return () => { alive = false; };
  }, []);

  useEffect(() => { chatEnd.current?.scrollIntoView({ behavior: "smooth", block: "end" }); }, [messages, sending]);

  async function sendMessage(event: FormEvent) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || sending) return;
    setError("");
    const next: Message[] = [...messages, { role: "user", text }];
    setMessages(next);
    setDraft("");
    setSending(true);
    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ messages: next.slice(-24) }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.detail || data.error || `Permintaan gagal (HTTP ${response.status})`);
      if (!data.answer) throw new Error("Gemini tidak mengirim jawaban.");
      setMessages([...next, { role: "model", text: data.answer }]);
    } catch (e) {
      setMessages(next);
      setError(e instanceof Error ? e.message : "Gagal menghubungi Gemini.");
    } finally { setSending(false); }
  }

  function chooseFile(selected: File | null) {
    setError("");
    setScanAnswer("");
    setFile(null);
    setPreview("");
    if (!selected) return;
    if (!["image/jpeg", "image/png", "image/webp"].includes(selected.type)) {
      setError("Gunakan gambar JPG, PNG, atau WEBP.");
      return;
    }
    if (selected.size > 2.8 * 1024 * 1024) {
      setError("Ukuran gambar maksimal 2,8 MB. Kompres gambar lalu coba lagi.");
      return;
    }
    setFile(selected);
    setPreview(URL.createObjectURL(selected));
  }

  async function analyzeImage() {
    if (!file || scanBusy) return;
    setError("");
    setScanAnswer("");
    setScanBusy(true);
    try {
      const base64 = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onerror = () => reject(new Error("Gagal membaca file gambar."));
        reader.onload = () => {
          const result = String(reader.result || "");
          const comma = result.indexOf(",");
          if (comma < 0) reject(new Error("Format gambar tidak valid."));
          else resolve(result.slice(comma + 1));
        };
        reader.readAsDataURL(file);
      });
      const response = await fetch("/api/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ prompt: scanPrompt.trim() || "Jelaskan isi gambar ini.", imageBase64: base64, mimeType: file.type }),
      });
      const data = await response.json();
      if (!response.ok) throw new Error(data.detail || data.error || `Analisis gagal (HTTP ${response.status})`);
      if (!data.answer) throw new Error("Gemini tidak mengirim hasil analisis.");
      setScanAnswer(data.answer);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Gagal menganalisis gambar.");
    } finally { setScanBusy(false); }
  }

  function resetChat() { setMessages([]); setDraft(""); setError(""); }

  return (
    <main className="app-shell">
      <header className="app-header">
        <a className="brand" href="/" aria-label="Aether">
          <span className="brand-mark">A</span><span>Aether</span>
        </a>
        <div className={`service-state ${service}`}><span className="state-dot" />{serviceMessage}</div>
      </header>

      <section className="workspace">
        <aside className="side-rail">
          <div className="rail-label">WORKSPACE</div>
          <button className={`nav-item ${tab === "chat" ? "active" : ""}`} onClick={() => { setTab("chat"); setError(""); }}>
            <span className="nav-icon">⌁</span><span>Chat Gemini</span>
          </button>
          <button className={`nav-item ${tab === "scan" ? "active" : ""}`} onClick={() => { setTab("scan"); setError(""); }}>
            <span className="nav-icon">▧</span><span>Scan gambar</span>
          </button>
          <div className="rail-bottom">
            <div className="rail-label">MODEL</div>
            <div className="model-chip"><span className="model-indicator" />Gemini 2.5 Flash</div>
            <p className="rail-note">Permintaan diproses melalui backend Aether. API key tetap tersimpan di server.</p>
          </div>
        </aside>

        <section className="main-panel">
          <div className="content-heading">
            <div>
              <div className="eyebrow">AETHER WORKSPACE</div>
              <h1>{tab === "chat" ? "Chat Gemini" : "Scan gambar"}</h1>
              <p>{tab === "chat" ? "Tanyakan sesuatu dan lanjutkan percakapan dengan Gemini." : "Pilih gambar yang ingin kamu bagikan untuk dianalisis Gemini."}</p>
            </div>
            {tab === "chat" && messages.length > 0 && <button className="text-button" onClick={resetChat}>Percakapan baru</button>}
          </div>

          {error && <div className="error-banner" role="alert"><strong>Terjadi kendala</strong><span>{error}</span></div>}

          {tab === "chat" ? (
            <div className="chat-workspace">
              <div className="conversation">
                {messages.length === 0 ? (
                  <div className="welcome-state">
                    <div className="welcome-mark">A</div>
                    <div className="eyebrow">GEMINI ASSISTANT</div>
                    <h2>Apa yang ingin kamu<br className="desktop-break"/> ketahui hari ini?</h2>
                    <p>Mulai percakapan baru. Kamu bisa bertanya, meminta penjelasan, atau berdiskusi tentang ide.</p>
                    <div className="suggestions">
                      {["Jelaskan konsep yang sulit dengan sederhana", "Bantu saya menyusun rencana proyek", "Buat ringkasan dari topik tertentu"].map((s) => <button key={s} onClick={() => setDraft(s)}>{s}<span>↗</span></button>)}
                    </div>
                  </div>
                ) : (
                  <div className="message-list">
                    {messages.map((m, i) => <article className={`message ${m.role === "user" ? "from-user" : "from-model"}`} key={`${i}-${m.role}`}>
                      <div className="message-avatar">{m.role === "user" ? "K" : "A"}</div>
                      <div className="message-body"><div className="message-author">{m.role === "user" ? "Kamu" : "Aether · Gemini"}</div><div className="message-text">{m.text}</div></div>
                    </article>)}
                    {sending && <article className="message from-model"><div className="message-avatar">A</div><div className="message-body"><div className="message-author">Aether · Gemini</div><div className="typing"><i/><i/><i/></div></div></article>}
                    <div ref={chatEnd}/>
                  </div>
                )}
              </div>
              <form className="composer" onSubmit={sendMessage}>
                <textarea value={draft} onChange={(e) => setDraft(e.target.value)} onKeyDown={(e) => { if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); void sendMessage(e as unknown as FormEvent); } }} placeholder="Tulis pesan untuk Gemini..." rows={2} maxLength={8000} disabled={sending || service !== "ready"} />
                <div className="composer-footer"><span>Enter untuk kirim · Shift + Enter untuk baris baru</span><button className="send-button" type="submit" disabled={!draft.trim() || sending || service !== "ready"}>{sending ? "Mengirim..." : "Kirim"} <span>↑</span></button></div>
              </form>
            </div>
          ) : (
            <div className="scan-workspace">
              <div className="scan-grid">
                <div className="scan-left">
                  <input ref={fileInput} type="file" accept="image/jpeg,image/png,image/webp" hidden onChange={(e) => chooseFile(e.target.files?.[0] || null)} />
                  <button className={`upload-zone ${preview ? "has-image" : ""}`} onClick={() => fileInput.current?.click()}>
                    {preview ? <img src={preview} alt="Pratinjau gambar yang dipilih"/> : <><span className="upload-icon">＋</span><strong>Pilih gambar untuk dianalisis</strong><span>JPG, PNG, atau WEBP · Maksimal 2,8 MB</span></>}
                  </button>
                  {file && <div className="file-meta"><span>{file.name}</span><button onClick={() => chooseFile(null)}>Hapus</button></div>}
                  <label className="field-label" htmlFor="scan-prompt">Instruksi analisis</label>
                  <textarea id="scan-prompt" className="scan-prompt" value={scanPrompt} onChange={(e) => setScanPrompt(e.target.value)} maxLength={8000} rows={3}/>
                  <button className="analyze-button" onClick={analyzeImage} disabled={!file || scanBusy || service !== "ready"}>{scanBusy ? "Menganalisis gambar..." : "Analisis gambar"} <span>↗</span></button>
                  <p className="privacy-note">Gambar hanya dikirim ke backend saat kamu menekan tombol Analisis gambar. Jangan unggah informasi pribadi atau gambar yang tidak berhak kamu bagikan.</p>
                </div>
                <div className="result-panel">
                  <div className="result-heading"><div><div className="eyebrow">GEMINI OUTPUT</div><h2>Hasil analisis</h2></div>{scanAnswer && <button className="text-button" onClick={() => setScanAnswer("")}>Bersihkan</button>}</div>
                  {scanAnswer ? <div className="result-text">{scanAnswer}</div> : <div className="empty-result"><div className="result-mark">✳</div><strong>Hasil akan muncul di sini</strong><span>Pilih gambar dan jalankan analisis untuk melihat respons Gemini.</span></div>}
                </div>
              </div>
            </div>
          )}
        </section>
      </section>
      <footer className="app-footer"><span>Aether · Gemini workspace</span><span>Gunakan hanya konten yang boleh kamu bagikan.</span></footer>
    </main>
  );
}
