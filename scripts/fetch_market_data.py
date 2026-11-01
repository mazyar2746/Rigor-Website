import urllib.request
import json
import time
import sys

# فقط ۵ نماد برای تست
SYMBOLS = {
    "فولاد": "46348559193224090",
    "خودرو": "65883838195688438",
    "خساپا": "44891482026867833",
    "شپنا": "7745894403636165",
    "وبملت": "778253364357513"
}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "fa-IR,fa;q=0.9,en;q=0.8",
}

def fetch_one(ins_code):
    url = f"https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/{ins_code}/0"
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"  ❌ خطا برای {ins_code}: {e}", file=sys.stderr)
        return None

def main():
    all_data = {}
    for name, code in SYMBOLS.items():
        print(f"در حال دریافت {name}...")
        data = fetch_one(code)
        if data and "closingPriceDaily" in data:
            all_data[name] = {
                "insCode": code,
                "prices": data["closingPriceDaily"][:30]
            }
            print(f"  ✅ {name} دریافت شد.")
        else:
            print(f"  ⚠️ {name} ناموفق.")
        
        time.sleep(3) # ۳ ثانیه استراحت بین هر درخواست

    with open("market_data.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)
    print(f"\n🏁 تمام شد. {len(all_data)} نماد ذخیره شد.")

if __name__ == "__main__":
    main()
