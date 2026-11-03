import urllib.request
import json
import time
import urllib.parse

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept": "application/json"
}

queries = ["ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی", "آ", "أ", "إ", "ئ", "ء"]

all_symbols = {}
print("در حال دریافت تمام نمادها...")

for q in queries:
    url = f"https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/{urllib.parse.quote(q)}"
    req = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=10) as res:
            data = json.loads(res.read().decode('utf-8'))
            if "instrumentSearch" in data:
                for item in data["instrumentSearch"]:
                    ins_code = item.get("insCode")
                    name = item.get("lVal18AFC", "").strip()
                    flow_title = item.get("flowTitle", "")
                    if ins_code and name:
                        if not name.endswith("ح") and "حق تقدم" not in name and "اوراق" not in name:
                            all_symbols[ins_code] = {"name": name, "flowTitle": flow_title}
    except Exception as e:
        pass
    time.sleep(0.2)

symbols_list = [{"name": v["name"], "insCode": k, "flowTitle": v["flowTitle"]} for k, v in all_symbols.items()]
with open("symbols_list_all.json", "w", encoding="utf-8") as f:
    json.dump(symbols_list, f, ensure_ascii=False, indent=2)

print(f"✅ {len(symbols_list)} نماد ذخیره شد.")
