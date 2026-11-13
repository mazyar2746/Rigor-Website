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
      // --- DEX Proxy for 0x API ---
      if (path.startsWith("/api/dex/quote")) {
        const params = url.searchParams.toString();
        const targetUrl = "https://api.0x.org/swap/permit2/quote?" + params;
        
        const r = await fetch(targetUrl, {
          headers: {
            "0x-api-key": "b3f49937-55a8-4f1d-ba9d-70c0bbb0c88e",
            "Content-Type": "application/json"
          }
        });
        
        if (!r.ok) {
            return new Response(JSON.stringify({ error: "0x API Error", status: r.status }), { status: r.status, headers: h });
        }
        return new Response(await r.text(), { headers: h });
      }

      // --- Crypto Route (for testing) ---
      if (path.startsWith("/api/crypto/list")) {
        const r = await fetch("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=250&page=1&sparkline=false");
        if (!r.ok) return new Response(JSON.stringify({ error: "CoinGecko Blocked" }), { status: 500, headers: h });
        return new Response(await r.text(), { headers: h });
      }

      return new Response("Not Found", { status: 404, headers: h });
    } catch (err) {
      return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: h });
    }
  }
};
