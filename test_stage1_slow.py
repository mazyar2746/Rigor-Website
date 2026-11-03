import urllib.request
import json
import time
import urllib.parse

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}

# فقط حروف اصلی که نمادهای مهم با اونا شروع میشن
queries = ["ف", "خ", "ش", "ب", "و", "ک", "م", "ن", "ا", "س", "پ", "ت", "ر", "د", "گ", "ل", "ه", "ی", "ق", "ج", "ز", "ع", "ح", "ط", "ص", "ض", "ظ", "ذ", "ث", "چ", "ژ", "آ"]

all_symbols = {}
print("⏳ در حال دریافت (آهسته و دقیق)...")

for i, q in enumerate(queries):
    url = f"https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/{urllib.parse.quote(q)}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            data = json.loads(res.read().decode('utf-8'))
            if "instrumentSearch" in data:
                count = 0
                for item in data["instrumentSearch"]:
                    ins_code = item.get("insCode")
                    name = item.get("lVal18AFC", "").strip()
                    flow_title = item.get("flowTitle", "")
                    if ins_code and name and not name.endswith("ح") and "حق تقدم" not in name and "اوراق" not in name:
                        all_symbols[ins_code] = {"name": name, "flowTitle": flow_title}
                        count += 1
                print(f"  [{i+1}/{len(queries)}] حرف '{q}': {count} نماد")
    except Exception as e:
        print(f"  [{i+1}/{len(queries)}] حرف '{q}': خطا")
    time.sleep(1.5)  # فاصله مهم!

symbols_list = [{"name": v["name"], "insCode": k, "flowTitle": v["flowTitle"]} for k, v in all_symbols.items()]
with open("symbols_stage1.json", "w", encoding="utf-8") as f:
    json.dump(symbols_list, f, ensure_ascii=False, indent=2)

print(f"\n✅ تعداد کل: {len(symbols_list)}")
print(f"فولاد؟ {any('فولاد' in x['name'] for x in symbols_list)}")
print(f"خودکفا؟ {any('خودكفا' in x['name'] for x in symbols_list)}")
