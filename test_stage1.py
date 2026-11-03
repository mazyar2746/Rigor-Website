import urllib.request
import json
import time
import urllib.parse

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept": "application/json"
}

# تمام حروف الفبا برای پوشش کامل
queries = ["ا", "ب", "پ", "ت", "ث", "ج", "چ", "ح", "خ", "د", "ذ", "ر", "ز", "ژ", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ک", "گ", "ل", "م", "ن", "و", "ه", "ی", "آ", "أ", "إ", "ئ", "ء", " "]

all_symbols = {}
print("⏳ در حال دریافت اطلاعات از TSETMC (لطفا صبر کنید)...")

for q in queries:
    # جستجوی همه نمادها با هر حرف
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
                    
                    # فقط حذف حق تقدم‌ها و اوراق (بقیه بازارها بمونن)
                    if ins_code and name:
                        if not name.endswith("ح") and "حق تقدم" not in name and "اوراق" not in name:
                            # با کد نماد به عنوان کلید ذخیره می‌کنیم تا تکراری‌ها حذف بشن
                            all_symbols[ins_code] = {
                                "name": name,
                                "flowTitle": flow_title
                            }
    except Exception as e:
        pass
    time.sleep(0.2)

# تبدیل به لیست
symbols_list = [{"name": v["name"], "insCode": k, "flowTitle": v["flowTitle"]} for k, v in all_symbols.items()]

# ذخیره فایل
with open("symbols_stage1.json", "w", encoding="utf-8") as f:
    json.dump(symbols_list, f, ensure_ascii=False, indent=2)

print(f"\n✅ تعداد کل نمادهای پیدا شده: {len(symbols_list)}")

# تست‌های مرحله ۱
print("\n🔍 تست‌های مرحله ۱:")
print(f"1. تعداد نمادها بیشتر از ۵۰۰ است؟ {len(symbols_list) > 500}")
print(f"2. آیا 'خودکفا' (با کاف عربی) در لیست هست؟ {any('خودكفا' in x['name'] for x in symbols_list)}")
print(f"3. آیا 'فولاد' در لیست هست؟ {any('فولاد' in x['name'] for x in symbols_list)}")

# نمایش توزیع بازارها
from collections import Counter
flows = Counter(item['flowTitle'] for item in symbols_list)
print("\n📊 توزیع بازارها:")
for flow, count in flows.most_common():
    print(f"  - {flow}: {count} نماد")
