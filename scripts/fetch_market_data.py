import urllib.request
import json
import time
import sys

# لیست نمادها (فعلاً فقط فولاد برای تست)
SYMBOLS = {
    "فولاد": "46348559193224090"
}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "fa-IR,fa;q=0.9,en;q=0.8",
}

def fetch_daily_price(ins_code):
    url = f"https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/{ins_code}/0"
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            data = json.loads(response.read().decode('utf-8'))
            return data
    except Exception as e:
        print(f"خطا در دریافت دیتا برای {ins_code}: {e}", file=sys.stderr)
        return None

def main():
    all_data = {}
    for name, code in SYMBOLS.items():
        print(f"در حال دریافت دیتای {name}...")
        data = fetch_daily_price(code)
        if data and "closingPriceDaily" in data:
            # فقط ۳۰ روز آخر رو ذخیره می‌کنیم که فایل سنگین نشه
            prices = data["closingPriceDaily"][:30]
            all_data[name] = {
                "insCode": code,
                "lastUpdate": time.strftime("%Y-%m-%d %H:%M:%S"),
                "prices": prices
            }
            print(f"✅ {name}: {len(prices)} رکورد دریافت شد.")
        else:
            print(f"❌ {name}: دیتایی دریافت نشد.")
        time.sleep(1)  # فاصله بین درخواست‌ها

    with open("market_data.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)
    
    print("✅ فایل market_data.json ساخته شد.")

if __name__ == "__main__":
    main()
