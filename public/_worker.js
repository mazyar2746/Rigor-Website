export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;
    const h = { 
      "Access-Control-Allow-Origin": "*", 
      "Content-Type": "application/json" 
    };

    if (request.method === "OPTIONS") return new Response(null, { headers: h });

    try {
      // --- DEX Proxy for 0x API (Fixed: WBNB address + v2 header) ---
      if (path.startsWith("/api/dex/quote")) {
        const params = url.searchParams.toString();
        const targetUrl = "https://api.0x.org/swap/permit2/quote?" + params;
        
        const r = await fetch(targetUrl, {
          headers: {
            "0x-api-key": "4b9b9fe6-dfde-4e5c-a89b-8ea0376b0950",
            "0x-version": "v2",
            "Content-Type": "application/json"
          }
        });
        
        if (!r.ok) {
            const errText = await r.text();
            return new Response(JSON.stringify({ error: "0x API Error", status: r.status, details: errText }), { status: r.status, headers: h });
        }
        return new Response(await r.text(), { headers: h });
      }

      // --- Crypto Route (Switched to Binance - no key, no block) ---
      if (path.startsWith("/api/crypto/list")) {
        const r = await fetch("https://api.binance.com/api/v3/ticker/24hr", {
          headers: {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Accept": "application/json"
          }
        });
        if (!r.ok) return new Response(JSON.stringify({ error: "Binance Blocked" }), { status: 500, headers: h });
        return new Response(await r.text(), { headers: h });
      }

      return new Response("Not Found", { status: 404, headers: h });
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: h });
    }
  }
};
