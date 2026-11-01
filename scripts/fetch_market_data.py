import urllib.request
import json
import sys

# API عمومی و معروف ir-market-data (روزانه آپدیت میشه)
# این API دیتای OHLC تمام نمادهای بورس رو برمی‌گردونه
URL = "https://api.ir-market-data.ir/api/v1/stock/daily"

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept": "application/json"
}

def main():
    print("در حال دریافت دیتای بازار از ir-market-data...")
    req = urllib.request.Request(URL, headers=HEADERS)
    
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            data = json.loads(response.read().decode('utf-8'))
            
            # ذخیره دیتا
            with open("market_data.json", "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            print(f"✅ دیتا با موفقیت دریافت شد.")
            
    except Exception as e:
        print(f"❌ خطا: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
