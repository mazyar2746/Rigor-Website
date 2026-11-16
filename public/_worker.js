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
            "0x-api-key": "کلید_0x_خودت",
            "0x-version": "v2",
            "Content-Type": "application/json"
          }
        });
        if (!r.ok) return new Response(JSON.stringify({ error: "0x API Error", status: r.status }), { status: r.status, headers: h });
        return new Response(await r.text(), { headers: h });
      }

      // --- 2. Full BSC Token List (Embedded - Never Blocked) ---
      if (path.startsWith("/api/dex/token-list")) {
        // لیست ۴۰ توکن برتر BSC که مستقیم تو کد هست
        const tokens = [
          { symbol: "BNB", name: "Binance Coin", address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", decimals: 18, chainId: 56 },
          { symbol: "USDT", name: "Tether USD", address: "0x55d398326f99059fF775485246999027B3197955", decimals: 18, chainId: 56 },
          { symbol: "BUSD", name: "Binance USD", address: "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56", decimals: 18, chainId: 56 },
          { symbol: "WBNB", name: "Wrapped BNB", address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", decimals: 18, chainId: 56 },
          { symbol: "CAKE", name: "PancakeSwap", address: "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", decimals: 18, chainId: 56 },
          { symbol: "XRP", name: "Ripple", address: "0x1D2F0da169ceB9fC7B3144628dB156f3F6c60dBE", decimals: 18, chainId: 56 },
          { symbol: "ADA", name: "Cardano", address: "0x3EE2200Efb3400fAbB9AacF31297cBdD1d435D47", decimals: 18, chainId: 56 },
          { symbol: "DOGE", name: "Dogecoin", address: "0xbA2aE424d960c26247Dd6c32edC70B295c744C43", decimals: 8, chainId: 56 },
          { symbol: "SHIB", name: "Shiba Inu", address: "0x2859e4544C4bB03966803b044A93563Bd2D0DD4D", decimals: 18, chainId: 56 },
          { symbol: "PEPE", name: "Pepe", address: "0x25d887Ce7a35172C62FeBFD67a1856F20FaEbB00", decimals: 18, chainId: 56 },
          { symbol: "TON", name: "Toncoin", address: "0x76A797A59Ba2C17726896976B7B3747BfD1d220f", decimals: 9, chainId: 56 },
          { symbol: "LINK", name: "Chainlink", address: "0xF8A0BF9cF54Bb92F17374d9e9A321E6a111a51bD", decimals: 18, chainId: 56 },
          { symbol: "DOT", name: "Polkadot", address: "0x7083609fCE4d1d8Dc0C979AAb8c869Ea2C873402", decimals: 10, chainId: 56 },
          { symbol: "MATIC", name: "Polygon", address: "0xCC42724C6683B7E57334c4E856f4c9965ED682bD", decimals: 18, chainId: 56 },
          { symbol: "LTC", name: "Litecoin", address: "0x4338665CBB7B2485A8855A139b75D5e34AB0DB94", decimals: 18, chainId: 56 },
          { symbol: "TRX", name: "TRON", address: "0x85EAC5Ac2F758618dFa09bD094f0Ee5C0CF1743A", decimals: 6, chainId: 56 },
          { symbol: "AVAX", name: "Avalanche", address: "0x1CE0c2827e2eF14D5C4f29a091d735A204794041", decimals: 18, chainId: 56 },
          { symbol: "ATOM", name: "Cosmos", address: "0x0Eb3a705fc54725037CC9e008bDede697f62F335", decimals: 18, chainId: 56 },
          { symbol: "UNI", name: "Uniswap", address: "0xBf5140A22578168FD562DCcF235E5D43A02ce9B1", decimals: 18, chainId: 56 },
          { symbol: "FIL", name: "Filecoin", address: "0x0D8Ce2A99Bb6e3B7Db580eD848240e4a0F9aE153", decimals: 18, chainId: 56 },
          { symbol: "INJ", name: "Injective", address: "0xa2B726B1145A4773F68593CF171187d8EBe4d495", decimals: 18, chainId: 56 },
          { symbol: "RUNE", name: "THORChain", address: "0xfA9343C3897324496A05fC75abeD6bAC29f8A40f", decimals: 18, chainId: 56 },
          { symbol: "APE", name: "ApeCoin", address: "0xC762043E211571eB34f1ef377e5e8e76914962f9", decimals: 18, chainId: 56 },
          { symbol: "TWT", name: "Trust Wallet", address: "0x4B0F1812e5Df2A09796481Ff14017e6005508003", decimals: 18, chainId: 56 },
          { symbol: "FTM", name: "Fantom", address: "0xAD29AbB318791D579433D831ed122aFeAf29dcfe", decimals: 18, chainId: 56 },
          { symbol: "SAND", name: "The Sandbox", address: "0x67b725d7e342d7B611fa85e859Df9697D9378B2e", decimals: 18, chainId: 56 },
          { symbol: "MANA", name: "Decentraland", address: "0x26433c8127d9b4e9B71Eaa15111DF99Ea2EeB2f8", decimals: 18, chainId: 56 },
          { symbol: "GALA", name: "Gala", address: "0x7dDEE176F665cD201F93eEDE625770E2fD911990", decimals: 18, chainId: 56 },
          { symbol: "XLM", name: "Stellar", address: "0x43C934A845205F0b514417d757d7235B8f71f1B9", decimals: 18, chainId: 56 },
          { symbol: "EOS", name: "EOS", address: "0x56b6fB708fC5733eD2C7CfaF522aBc2D2B3D3C2D", decimals: 18, chainId: 56 },
          { symbol: "BCH", name: "Bitcoin Cash", address: "0x8fF795a6F4D97E7887C79beA79aba5cc76444aDf", decimals: 18, chainId: 56 },
          { symbol: "ALGO", name: "Algorand", address: "0x7B5c6f6bD9B7A410b7c56E5f4D7bE6c43Dd4d2A1", decimals: 18, chainId: 56 },
          { symbol: "VET", name: "VeChain", address: "0x6FDcdfef7c496407cCb0cEC90f9C5Aaa1Cc8D888", decimals: 18, chainId: 56 },
          { symbol: "NEAR", name: "NEAR Protocol", address: "0x1Fa4a73a3F0133f0025378af00236f3aBDEE5D63", decimals: 18, chainId: 56 },
          { symbol: "EGLD", name: "MultiversX", address: "0xbF7c81FFF98BbE61B40Ed186e4AfD6DDd01337fe", decimals: 18, chainId: 56 },
          { symbol: "AXS", name: "Axie Infinity", address: "0x715D400F88C167884bbCc41C5FeA407ed4D2f8A0", decimals: 18, chainId: 56 },
          { symbol: "HOT", name: "Holo", address: "0xC1357d32F19aB3FB0ce82d7865fDa68e153Ae3D9", decimals: 18, chainId: 56 },
          { symbol: "ZIL", name: "Zilliqa", address: "0xb86AbCb37C3A4B64f74f59301AFF131a1BEcC787", decimals: 12, chainId: 56 },
          { symbol: "1INCH", name: "1inch", address: "0x111111111117dC0aa78b770fA6A738034120C302", decimals: 18, chainId: 56 },
          { symbol: "COMP", name: "Compound", address: "0x52CE071Bd9b1C4B00A0b92D298c512478CaD67e8", decimals: 18, chainId: 56 }
        ];
        return new Response(JSON.stringify(tokens), { headers: h });
      }

      // --- 3. DEX Chart (OKX API) ---
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

      // --- 4. Crypto List (OKX) ---
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

      // --- 5. Token Lookup by Contract Address ---
      if (path.startsWith("/api/dex/lookup-token")) {
        const address = url.searchParams.get("address") || "";
        if (!address) return new Response(JSON.stringify({found: false, error: "address required"}), {status: 400, headers: h});
        
        // جستجو تو لیست ثابت بالا
        const tokens = [
          { symbol: "USDT", name: "Tether USD", address: "0x55d398326f99059fF775485246999027B3197955", decimals: 18, chainId: 56 },
          { symbol: "BNB", name: "Binance Coin", address: "0xbb4CdB9CBd36B01bD1cBaEBF2De08d9173bc095c", decimals: 18, chainId: 56 },
          { symbol: "BUSD", name: "Binance USD", address: "0xe9e7CEA3DedcA5984780Bafc599bD69ADd087D56", decimals: 18, chainId: 56 },
          { symbol: "CAKE", name: "PancakeSwap", address: "0x0E09FaBB73Bd3Ade0a17ECC321fD13a19e81cE82", decimals: 18, chainId: 56 },
          { symbol: "DOGE", name: "Dogecoin", address: "0xbA2aE424d960c26247Dd6c32edC70B295c744C43", decimals: 8, chainId: 56 }
        ];
        
        const normalized = address.toLowerCase();
        const match = tokens.find(t => t.address.toLowerCase() === normalized);
        
        if (match) {
            return new Response(JSON.stringify({found: true, symbol: match.symbol, name: match.name, address: match.address, chainId: match.chainId, network: "BSC", decimals: match.decimals}), {headers: h});
        }
        return new Response(JSON.stringify({found: false, error: "Token not found"}), {headers: h});
      }

      return new Response("Not Found", { status: 404, headers: h });
    } catch (err) {
        return new Response(JSON.stringify({ error: err.message }), { status: 500, headers: h });
    }
  }
};
