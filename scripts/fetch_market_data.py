import urllib.request
import json
import sys
import time

# آدرس دیتای آماده از ریپوی tse-data (روزانه آپدیت میشه)
# این فایل شامل دیتای OHLC و حجم معاملات تمام نمادهاست
URL = "https://raw.githubusercontent.com/hamidsafari/tse-data/main/data/daily.json"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
}

def fetch_all_data():
    print("در حال دریافت دیتای کل بازار از tse-data...")
    req = urllib.request.Request(URL, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            data = json.loads(response.read().decode('utf-8'))
            return data
    except Exception as e:
        print(f"خطا در دریافت دیتا: {e}", file=sys.stderr)
        return None

def main():
    data = fetch_all_data()
    if data:
        # ذخیره کل دیتای بازار در فایل JSON
        with open("market_data.json", "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"✅ فایل market_data.json با موفقیت ساخته شد. (تعداد رکورد: {len(data)})")
    else:
        print("❌ دریافت دیتا با شکست مواجه شد.")
        sys.exit(1)

if __name__ == "__main__":
    main()
