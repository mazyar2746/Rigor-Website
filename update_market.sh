import urllib.parse
#!/data/data/com.termux/files/usr/bin/bash

echo "🔄 شروع دریافت دیتای بورس..."

python3 - << 'PYEOF'
import urllib.request
import json
import time

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept": "application/json"
}

# ۱. دریافت لیست نمادها با جستجوی حروف مختلف الفبا (برای پوشش کامل)
print("در حال دریافت لیست نمادها...")
all_symbols_dict = {} # برای حذف تکراری‌ها

# لیست حروفی که توی اسم نمادها زیاد استفاده میشن
search_queries = ["ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی"]

for query in search_queries:
    url = f"https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/{urllib.parse.quote(query)}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            raw_data = json.loads(res.read().decode('utf-8'))
            if "instrumentSearch" in raw_data:
                for item in raw_data["instrumentSearch"]:
                    name = item.get("lVal18AFC", "")
                    ins_code = item.get("insCode", "")
                    if name and ins_code:
                        all_symbols_dict[name] = ins_code
    except Exception as e:
        pass # اگه یه حرف خطا داد، بقیه رو ادامه بده
    time.sleep(0.5)

# تبدیل دیکشنری به لیست
all_symbols = [{"name": k, "insCode": v} for k, v in all_symbols_dict.items()]

with open("symbols_list.json", "w", encoding="utf-8") as f:
    json.dump(all_symbols, f, ensure_ascii=False, indent=2)
print(f"✅ لیست {len(all_symbols)} نماد ذخیره شد.")

# ۲. دریافت دیتای ۱۵ نماد تاپ
SYMBOLS = {
    "فولاد": "46348559193224090", "خودرو": "65883838195688438", "خساپا": "44891482026867833",
    "شپنا": "7745894403636165", "وبملت": "778253364357513", "شستا": "2400322364771558",
    "فملی": "35425587644337450", "کگل": "23343799233593953", "شتران": "34736172400344808",
    "برکت": "35366681030756042", "وپاسار": "63917421733088075", "شبندر": "21317642344546876",
    "اخابر": "51411715166455548", "کچاد": "35700344742885862", "همراه": "68635710163497089"
}

all_data = {}
for name, code in SYMBOLS.items():
    print(f"دریافت {name}...")
    url = f"https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/{code}/0"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            data = json.loads(res.read().decode('utf-8'))
            if "closingPriceDaily" in data:
                all_data[name] = {
                    "insCode": code,
                    "prices": data["closingPriceDaily"][:30]
                }
                print(f"  ✅ موفق")
    except Exception as e:
        print(f"  ❌ خطا: {e}")
    time.sleep(1)

with open("market_data.json", "w", encoding="utf-8") as f:
    json.dump(all_data, f, ensure_ascii=False, indent=2)

print(f"\n🏁 تمام شد. {len(all_data)} نماد ذخیره شد.")
PYEOF

echo "📤 در حال پوش به گیت‌هاب..."
git add market_data.json symbols_list.json
git commit -m "Update market data & full symbols list $(date +%Y-%m-%d)"
git push origin feature/iran-stock-analyzer

echo "✅ آپدیت کامل شد!"
