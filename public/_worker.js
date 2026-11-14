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
      // --- 1. DEX Swap Proxy (0x API) ---
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

      // --- 2. DEX Chart (OKX API) ---
      if (path.startsWith("/api/dex/chart")) {
        const symbol = url.searchParams.get("symbol") || "BNB-USDT";
        const interval = url.searchParams.get("interval") || "1H";
        const limit = url.searchParams.get("limit") || "100";
        let bar = "1H";
        if (interval === "1h") bar = "1H";
        else if (interval === "4h") bar = "4H";
        else if (interval === "1d") bar = "1D";

        const targetUrl = `https://www.okx.com/api/v5/market/candles?instId=${symbol}&bar=${bar}&limit=${limit}`;
        const r = await fetch(targetUrl, {
          headers: { "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)" }
        });
        if (!r.ok) return new Response(JSON.stringify({ error: "OKX Chart Blocked" }), { status: 500, headers: h });
        return new Response(await r.text(), { headers: h });
      }

      // --- 3. Crypto List (OKX) ---
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

      // --- 4. Single Token Lookup (BSC) ---
      if (path.startsWith("/api/dex/lookup-token")) {
        const address = url.searchParams.get("address") || "";
        if (!address) return new Response(JSON.stringify({found: false, error: "address required"}), {status: 400, headers: h});
        try {
            const r = await fetch("https://tokens.pancakeswap.finance/pancakeswap-extended.json", {headers: {"User-Agent": "Mozilla/5.0"}});
            if (!r.ok) return new Response(JSON.stringify({found: false, error: "Token list blocked"}), {status: 500, headers: h});
            const data = await r.json();
            const normalized = address.toLowerCase();
            const match = (data.tokens || []).find(t => t.address.toLowerCase() === normalized);
            if (match) {
                return new Response(JSON.stringify({found: true, symbol: match.symbol, name: match.name, address: match.address, network: "BSC", decimals: match.decimals}), {headers: h});
            }
            return new Response(JSON.stringify({found: false, error: "Token not found"}), {headers: h});
        } catch (e) {
            return new Response(JSON.stringify({found: false, error: e.message}), {status: 500, headers: h});
        }
      }

      // --- 5. Multi-Chain Token Lookup ---
      if (path.startsWith("/api/dex/lookup-token-multi")) {
        const address = url.searchParams.get("address") || "";
        if (!address) return new Response(JSON.stringify({found: false, error: "address required"}), {status: 400, headers: h});
        
        const normalized = address.toLowerCase();
        const networks = [
            { chainId: 56, name: "BSC", url: "https://raw.githubusercontent.com/pancakeswap/pancake-default-token-list/master/build/pancakeswap-extended.json" },
            { chainId: 1, name: "Ethereum", url: "https://raw.githubusercontent.com/Uniswap/default-token-list/main/build/uniswap-default.tokenlist.json" }
        ];
        
        for (const net of networks) {
            try {
                const r = await fetch(net.url, { headers: {"User-Agent": "Mozilla/5.0"} });
                if (!r.ok) continue;
                const data = await r.json();
                const tokens = data.tokens || [];
                const match = tokens.find(t => t.address.toLowerCase() === normalized);
                if (match) {
                    return new Response(JSON.stringify({found: true, symbol: match.symbol, name: match.name, address: match.address, chainId: net.chainId, network: net.name, decimals: match.decimals}), {headers: h});
                }
            } catch (e) { continue; }
        }
        return new Response(JSON.stringify({found: false, error: "Token not found on any supported network"}), {headers: h});
      }

      return new Response("Not Found", { status: 404, headers: h });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: h });
    }
  }
};
