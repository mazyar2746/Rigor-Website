export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;
    const h = { "Access-Control-Allow-Origin": "*", "Content-Type": "application/json" };

    if (request.method === "OPTIONS") return new Response(null, { headers: h });

    try {
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
        if (!r.ok) return new Response(JSON.stringify({ error: "0x API Error", status: r.status }), { status: r.status, headers: h });
        return new Response(await r.text(), { headers: h });
      }

      if (path.startsWith("/api/crypto/list")) {
        const r = await fetch("https://www.okx.com/api/v5/market/tickers?instType=SPOT", {
          headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" }
        });
        if (!r.ok) return new Response(JSON.stringify({ error: "OKX Blocked" }), { status: 500, headers: h });
        
        const json = await r.json();
        const mapped = [];
        if (json && json.data) {
            for (let i = 0; i < json.data.length; i++) {
                const item = json.data[i];
                if (item.instId && item.instId.endsWith("-USDT")) {
                    const symbol = item.instId.split("-")[0].toLowerCase();
                    mapped.push({
                        id: symbol,
                        symbol: symbol,
                        name: symbol.toUpperCase(),
                        image: "https://assets.coincap.io/assets/icons/" + symbol + "@2x.png",
                        current_price: parseFloat(item.last) || 0,
                        market_cap: 0,
                        price_change_percentage_24h: 0
                    });
                }
            }
        }
        return new Response(JSON.stringify(mapped), { headers: h });
      }

      return new Response("Not Found", { status: 404, headers: h });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: h });
    }
  }
};
