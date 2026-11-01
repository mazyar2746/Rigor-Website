import pytse_client as tse
import json
import sys

# لیست نمادهای پرطرفدار
SYMBOLS = [
    "فولاد", "خودرو", "خساپا", "شپنا", "وبملت", "شستا",
    "فملی", "کگل", "شتران", "برکت", "وپاسار", "شبندر",
    "اخابر", "کچاد", "همراه", "شپدیس", "نوری", "وغدیر"
]

def main():
    try:
        print("در حال دریافت دیتای بازار با pytse-client...")
        # این کتابخانه خودش مدیریت درخواست‌ها رو انجام میده
        tickers = tse.download(symbols=SYMBOLS, write_to_csv=False)
        
        result = {}
        for symbol, df in tickers.items():
            # فقط ۳۰ روز آخر و ستون‌های ضروری
            if df is not None and len(df) > 0:
                last_30 = df.tail(30)
                result[symbol] = last_30.to_dict(orient="records")
                print(f"✅ {symbol}: {len(last_30)} رکورد")
        
        with open("market_data.json", "w", encoding="utf-8") as f:
            json.dump(result, f, ensure_ascii=False, indent=2)
        
        print(f"🏁 تمام شد. {len(result)} نماد ذخیره شد.")
        
    except Exception as e:
        print(f"❌ خطا: {e}", file=sys.stderr)
        sys.exit(1)

if __name__ == "__main__":
    main()
