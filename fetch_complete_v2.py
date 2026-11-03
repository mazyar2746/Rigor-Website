import urllib.request
import json

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"}

print("در حال دریافت دیتای کامل...")
url = "https://old.tsetmc.com/tsev2/data/MarketWatchPlus.aspx?t=0&r=0"
req = urllib.request.Request(url, headers=headers)

try:
    with urllib.request.urlopen(req, timeout=30) as res:
        data = res.read().decode('utf-8', errors='ignore')
    
    # تقسیم با @ - بخش سوم (index 2) شامل نمادهاست
    parts = data.split('@')
    if len(parts) < 3:
        print(f"❌ فقط {len(parts)} بخش پیدا شد. نیاز به حداقل ۳ بخش.")
        exit()
    
    symbols_section = parts[2]
    rows = symbols_section.split(';')
    
    all_symbols = []
    seen_codes = set()
    
    for row in rows:
        if not row.strip():
            continue
        fields = row.split(',')
        if len(fields) < 4:
            continue
        
        ins_code = fields[0].strip()
        isin = fields[1].strip()
        short_name = fields[2].strip()
        full_name = fields[3].strip() if len(fields) > 3 else ""
        
        # فیلترها
        if not ins_code or not short_name:
            continue
        if short_name.endswith('ح'):  # حق تقدم
            continue
        if "اوراق" in full_name or "مشاركت" in full_name or "مرابحه" in full_name or "صكوک" in full_name or "اجاره" in full_name:
            continue
        if ins_code in seen_codes:
            continue
        
        seen_codes.add(ins_code)
        all_symbols.append({
            "name": short_name,
            "fullName": full_name,
            "insCode": ins_code,
            "isin": isin
        })
    
    with open("symbols_complete.json", "w", encoding="utf-8") as f:
        json.dump(all_symbols, f, ensure_ascii=False, indent=2)
    
    print(f"\n✅ {len(all_symbols)} نماد ذخیره شد.")
    print(f"فولاد؟ {any('فولاد' in x['name'] for x in all_symbols)}")
    print(f"خودکفا؟ {any('خودكفا' in x['name'] for x in all_symbols)}")
    print(f"ره‌آورد؟ {any('ره' in x['name'] for x in all_symbols)}")
    print(f"پیزد؟ {any('پیزد' in x['name'] for x in all_symbols)}")
    print(f"شپلی؟ {any('شپلي' in x['name'] for x in all_symbols)}")
    
    # نمایش ۵ نماد اول
    print(f"\n۵ نماد اول:")
    for s in all_symbols[:5]:
        print(f"  {s['name']} - {s['insCode']}")
        
except Exception as e:
    print(f"❌ خطا: {e}")
