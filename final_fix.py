import urllib.request
import json
import time
import urllib.parse

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}

# خواندن لیست فعلی (۵۱۶ نماد)
with open("symbols_stage1_complete.json", "r", encoding="utf-8") as f:
    symbols_list = json.load(f)

existing_codes = {item["insCode"] for item in symbols_list}
print(f"لیست فعلی: {len(symbols_list)} نماد")

# حروف مهم که ممکنه خطا داده باشن
queries = ["خ", "پ", "ف", "ش", "ب", "و", "ک", "م", "ن", "ا", "س", "ت", "ر", "د", "گ", "ل", "ه", "ی", "ق", "ج", "ز", "ع", "ح", "ط", "ص", "ض", "ظ", "ذ", "ث", "چ", "ژ", "آ"]

new_count = 0
for q in queries:
    # برای هر حرف، تا ۳ بار تلاش می‌کنیم
    for attempt in range(3):
        url = f"https://cdn.tsetmc.com/api/Instrument/GetInstrumentSearch/{urllib.parse.quote(q)}"
        req = urllib.request.Request(url, headers=headers)
        try:
            with urllib.request.urlopen(req, timeout=20) as res:
                data = json.loads(res.read().decode('utf-8'))
                if "instrumentSearch" in data:
                    for item in data["instrumentSearch"]:
                        ins_code = item.get("insCode")
                        name = item.get("lVal18AFC", "").strip()
                        flow_title = item.get("flowTitle", "")
                        if ins_code and name and not name.endswith("ح") and "حق تقدم" not in name and "اوراق" not in name:
                            if ins_code not in existing_codes:
                                symbols_list.append({"name": name, "insCode": ins_code, "flowTitle": flow_title})
                                existing_codes.add(ins_code)
                                new_count += 1
                                print(f"  ✅ {name} اضافه شد (بازار: {flow_title})")
                break  # اگه موفق بود، از حلقه تلاش خارج شو
        except Exception as e:
            if attempt < 2:
                time.sleep(2)  # قبل از تلاش مجدد صبر کن
            else:
                print(f"  ❌ حرف '{q}' بعد از ۳ تلاش خطا داد")
    time.sleep(1.5)  # فاصله بین حروف

# ذخیره لیست نهایی
with open("symbols_final.json", "w", encoding="utf-8") as f:
    json.dump(symbols_list, f, ensure_ascii=False, indent=2)

print(f"\n✅ {new_count} نماد جدید اضافه شد.")
print(f"تعداد نهایی: {len(symbols_list)}")
print(f"فولاد؟ {any('فولاد' in x['name'] for x in symbols_list)}")
print(f"خودکفا؟ {any('خودكفا' in x['name'] for x in symbols_list)}")
print(f"پیزد؟ {any('پیزد' in x['name'] for x in symbols_list)}")
