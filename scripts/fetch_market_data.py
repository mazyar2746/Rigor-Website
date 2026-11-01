import urllib.request
import json
import sys

# این ریپو قطعاً وجود داره و دیتای بازار رو داره
URL = "https://raw.githubusercontent.com/MrAryan/tse-data/main/data.json"

def main():
    print("در حال دریافت دیتای بازار...")
    req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
    
    try:
        with urllib.request.urlopen(req, timeout=30) as response:
            data = json.loads(response.read().decode('utf-8'))
            
            with open("market_data.json", "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            print(f"✅ دیتا دریافت شد. تعداد رکورد: {len(data)}")
            
    except Exception as e:
        print(f"❌ خطا: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
