import urllib.request
import json
import sys

# آدرس فایل JSON آماده از ریپوی معروف (روزانه آپدیت میشه)
# این ریپو دیتای OHLC و حجم کل بورس رو داره
URL = "https://raw.githubusercontent.com/mehdi-torabiv/iran-stock-data/main/data/daily/latest.json"

HEADERS = {
    "User-Agent": "Mozilla/5.0",
    "Accept": "application/json"
}

def main():
    print("در حال دریافت دیتای بازار از ریپوی آماده...")
    req = urllib.request.Request(URL, headers=HEADERS)
    
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            data = json.loads(response.read().decode('utf-8'))
            
            # ذخیره دیتا
            with open("market_data.json", "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            print(f"✅ دیتا با موفقیت دریافت شد. تعداد نمادها: {len(data) if isinstance(data, list) else 'N/A'}")
            
    except Exception as e:
        print(f"❌ خطا: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
