export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    const path = url.pathname;

    const corsHeaders = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "GET, HEAD, POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    try {
      if (path.startsWith("/api/crypto")) {
        let targetUrl = "";
        if (path === "/api/crypto") {
          targetUrl = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=false";
        } else if (path === "/api/crypto-detail") {
          const coinId = url.searchParams.get("id") || "bitcoin";
          targetUrl = `https://api.coingecko.com/api/v3/coins/${coinId}/market_chart?vs_currency=usd&days=7&interval=daily`;
        }

        const response = await fetch(targetUrl, {
          headers: {
            "Accept": "application/json",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"
          }
        });

        if (!response.ok) throw new Error(`CoinGecko Error: ${response.status}`);
        const text = await response.text();
        if (!text) throw new Error("Empty response from CoinGecko");
        
        return new Response(text, {
          headers: { ...corsHeaders, "Content-Type": "application/json" }
        });
      }

      if (path.startsWith("/api/stock")) {
        const tsetmcHeaders = {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
          "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
          "Accept-Language": "fa-IR,fa;q=0.9,en-US;q=0.8,en;q=0.7",
          "Referer": "http://www.tsetmc.com/",
          "Origin": "http://www.tsetmc.com"
        };

        let targetUrl = "";
        
        if (path === "/api/stock-list") {
          targetUrl = "http://old.tsetmc.com/tsev2/data/MarketWatchPlus.aspx";
        } else if (path === "/api/stock-detail") {
          const insCode = url.searchParams.get("insCode") || "46348559193224090";
          targetUrl = `http://old.tsetmc.com/tsev2/data/instinfodata.aspx?i=${insCode}&c=57`;
        } else if (path === "/api/stock-history") {
          const insCode = url.searchParams.get("insCode") || "46348559193224090";
          targetUrl = `http://old.tsetmc.com/tsev2/data/ClosingPrice.aspx?i=${insCode}&c=57`;
        }

        const response = await fetch(targetUrl, { headers: tsetmcHeaders });
        if (!response.ok) throw new Error(`TSETMC Error: ${response.status}`);
        const text = await response.text();
        
        return new Response(text, {
          headers: { ...corsHeaders, "Content-Type": "text/plain; charset=utf-8" }
        });
      }

      if (path === "/api/market-index") {
        const targetUrl = "http://old.tsetmc.com/tsev2/data/MarketWatchInit.aspx?h=0&r=0";
        const response = await fetch(targetUrl, {
          headers: {
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0",
            "Referer": "http://www.tsetmc.com/"
          }
        });
        const text = await response.text();
        return new Response(text, {
          headers: { ...corsHeaders, "Content-Type": "text/plain; charset=utf-8" }
        });
      }

      return env.ASSETS ? env.ASSETS.fetch(request) : new Response("Not Found", { status: 404 });

    } catch (error) {
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
        headers: { ...corsHeaders, "Content-Type": "application/json" }
      });
    }
  },
};
