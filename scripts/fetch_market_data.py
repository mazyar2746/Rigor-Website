import urllib.request
import json
import time
import sys

# لیست ۳۰ نماد پرطرفدار بورس و فرابورس ایران
SYMBOLS = {
    "فولاد": "46348559193224090",
    "خودرو": "65883838195688438",
    "خساپا": "44891482026867833",
    "شپنا": "7745894403636165",
    "وبملت": "778253364357513",
    "شستا": "2400322364771558",
    "فملی": "35425587644337450",
    "کگل": "23343799233593953",
    "شتران": "34736172400344808",
    "برکت": "35366681030756042",
    "وپاسار": "63917421733088075",
    "شبندر": "21317642344546876",
    "اخابر": "51411715166455548",
    "کچاد": "35700344742885862",
    "همراه": "68635710163497089",
    "شپدیس": "36604253128626791",
    "نوری": "19040514831923530",
    "وغدیر": "47938801729052923",
    "شسپا": "64508524476360003",
    "کروی": "59266731389636280",
    "شوینده": "30438660716428706",
    "ونیکی": "61906713595156684",
    "فاسمین": "30719966126741850",
    "بیمه": "44358460271556699",
    "کرماشا": "12400767033721731",
    "شلعاب": "43636871246794766",
    "وپترو": "28273137859866504",
    "سیمانی": "64999324184882315",
    "پترول": "33449931919452387",
    "وغرب": "36866174786467164"
}

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "application/json, text/plain, */*",
    "Accept-Language": "fa-IR,fa;q=0.9,en;q=0.8",
}

def fetch_daily_price(ins_code):
    url = f"https://cdn.tsetmc.com/api/ClosingPrice/GetClosingPriceDailyList/{ins_code}/0"
    req = urllib.request.Request(url, headers=HEADERS)
    try:
        with urllib.request.urlopen(req, timeout=15) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"خطا در دریافت دیتا برای {ins_code}: {e}", file=sys.stderr)
        return None

def main():
    all_data = {}
    for name, code in SYMBOLS.items():
        print(f"در حال دریافت دیتای {name}...")
        data = fetch_daily_price(code)
        if data and "closingPriceDaily" in data:
            prices = data["closingPriceDaily"][:30] # ۳۰ روز آخر
            all_data[name] = {
                "insCode": code,
                "lastUpdate": time.strftime("%Y-%m-%d %H:%M:%S"),
                "prices": prices
            }
            print(f"✅ {name}: {len(prices)} رکورد دریافت شد.")
        else:
            print(f"❌ {name}: دیتایی دریافت نشد.")
        time.sleep(0.5) # نیم ثانیه فاصله برای فشار نیاوردن به سرور

    with open("market_data.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)
    
    print(f"✅ فایل market_data.json با {len(all_data)} نماد ساخته شد.")

if __name__ == "__main__":
    main()
