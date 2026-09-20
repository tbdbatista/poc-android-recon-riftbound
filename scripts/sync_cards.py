import os
import json
import ssl
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor, as_completed

# Disable SSL verification checks for CDN
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
WORKSPACE_ROOT = os.path.dirname(SCRIPT_DIR)
ASSETS_DIR = os.path.join(WORKSPACE_ROOT, "app", "src", "main", "assets")
IMAGES_DIR = os.path.join(ASSETS_DIR, "images")
RAW_JSON_PATH = os.path.join(SCRIPT_DIR, "all_cards_raw.json")
TARGET_JSON_PATH = os.path.join(ASSETS_DIR, "all_cards.json")

os.makedirs(IMAGES_DIR, exist_ok=True)

def fetch_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, context=ctx, timeout=30) as response:
        return json.loads(response.read().decode())

def download_all_raw_cards():
    print("Fetching all cards from Riftcodex API...")
    all_cards = []
    page = 1
    size = 100
    total_pages = 1
    
    while page <= total_pages:
        url = f"https://api.riftcodex.com/cards?page={page}&size={size}"
        try:
            resp = fetch_json(url)
            items = resp.get("items", [])
            total_pages = resp.get("pages", 1)
            all_cards.extend(items)
            print(f" -> Page {page}/{total_pages} fetched ({len(items)} cards). Total: {len(all_cards)}")
            page += 1
            time.sleep(0.2)
        except Exception as e:
            print(f"Error fetching page {page}: {e}")
            break
            
    print(f"Total raw cards downloaded: {len(all_cards)}")
    with open(RAW_JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(all_cards, f, ensure_ascii=False, indent=2)
    return all_cards

def download_image(card):
    card_id = card.get("id")
    media = card.get("media", {})
    if not media:
        return card_id, False, "No media"
    
    url = media.get("image_url")
    if not url:
        return card_id, False, "No image url"
        
    destination = os.path.join(IMAGES_DIR, f"{card_id}.png")
    
    if os.path.exists(destination) and os.path.getsize(destination) > 1000:
        return card_id, True, "Already exists"
        
    for attempt in range(3):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
            with urllib.request.urlopen(req, context=ctx, timeout=30) as response:
                with open(destination, "wb") as out_file:
                    out_file.write(response.read())
            return card_id, True, "Downloaded"
        except Exception as e:
            if attempt == 2:
                return card_id, False, str(e)
            time.sleep(1)

def extract_collector_number(card):
    rb_id = card.get("riftbound_id", "")
    parts = rb_id.split("-")
    if len(parts) >= 2 and parts[1]:
        return parts[1]
    raw_num = card.get("collector_number")
    if raw_num is not None:
        return str(raw_num)
    return "0"

def build_mapped_cards(raw_cards):
    print("Mapping cards to local database schema...")
    raw_cards.sort(key=lambda x: (x.get("set", {}).get("set_id", ""), x.get("name", "")))
    
    mapped_cards = []
    for index, card in enumerate(raw_cards):
        card_id = card.get("id")
        set_info = card.get("set", {})
        set_id = set_info.get("set_id", "Unknown").upper()
        set_label = set_info.get("label", set_id)
        
        col_num = extract_collector_number(card)
        
        energy = card.get("attributes", {}).get("energy", 0) or 0
        might = card.get("attributes", {}).get("might", 0) or card.get("attributes", {}).get("power", 0) or 0
        
        tags_list = card.get("tags", []) or []
        classification = card.get("classification", {}) or {}
        type_tag = classification.get("type", "")
        rarity_tag = classification.get("rarity", "")
        domains_tags = classification.get("domain", []) or []
        
        combined_tags = tags_list + [type_tag, rarity_tag] + domains_tags
        combined_tags = [str(t) for t in combined_tags if t]
        tags_string = ", ".join(combined_tags)
        
        text_plain = card.get("text", {}).get("plain", "") if isinstance(card.get("text"), dict) else ""
        
        mapped_card = {
            "id": index + 1,
            "string_id": card_id,
            "name": card.get("name", "Unknown"),
            "set": set_id,
            "setName": set_label,
            "setCode": set_id,
            "collectorNumber": col_num,
            "energyCost": energy,
            "power": might,
            "tags": tags_string,
            "text": text_plain,
            "imageUrl": f"images/{card_id}.png"
        }
        mapped_cards.append(mapped_card)
        
    return mapped_cards

def append_custom_tokens_and_runes(mapped_cards):
    print("Checking and ensuring cloned runes/tokens are present...")
    current_keys = {(c["setCode"].upper(), str(c["collectorNumber"]).lower()) for c in mapped_cards}
    max_id = max((c["id"] for c in mapped_cards), default=0)
    
    # Templates from existing cards
    fury_tpl = next((c for c in mapped_cards if c["name"] == "Fury Rune" and c["setCode"] == "OGN"), None)
    calm_tpl = next((c for c in mapped_cards if c["name"] == "Calm Rune" and c["setCode"] == "OGN"), None)
    mind_tpl = next((c for c in mapped_cards if c["name"] == "Mind Rune" and c["setCode"] == "OGN"), None)
    body_tpl = next((c for c in mapped_cards if c["name"] == "Body Rune" and c["setCode"] == "OGN"), None)
    chaos_tpl = next((c for c in mapped_cards if c["name"] == "Chaos Rune" and c["setCode"] == "OGN"), None)
    order_tpl = next((c for c in mapped_cards if c["name"] == "Order Rune" and c["setCode"] == "OGN"), None)
    sprite_tpl = next((c for c in mapped_cards if "Sprite" in c["name"] and c["setCode"] == "OGN"), None)
    
    def make_clone(tpl, set_code, set_name, col_num, custom_name=None):
        nonlocal max_id
        if (set_code.upper(), col_num.lower()) in current_keys:
            return None
        max_id += 1
        clone = tpl.copy() if tpl else {}
        clone["id"] = max_id
        clone["set"] = set_code
        clone["setName"] = set_name
        clone["setCode"] = set_code
        clone["collectorNumber"] = col_num
        if custom_name:
            clone["name"] = custom_name
        current_keys.add((set_code.upper(), col_num.lower()))
        return clone

    extra_clones = []
    if fury_tpl and calm_tpl and mind_tpl and body_tpl and chaos_tpl and order_tpl:
        for s_code, s_name in [("UNL", "Unleashed"), ("SFD", "Spiritforged")]:
            for tpl, r_num in [(fury_tpl, "r01"), (calm_tpl, "r02"), (mind_tpl, "r03"), (body_tpl, "r04"), (chaos_tpl, "r05"), (order_tpl, "r06")]:
                c = make_clone(tpl, s_code, s_name, r_num)
                if c: extra_clones.append(c)

    if sprite_tpl:
        c = make_clone(sprite_tpl, "UNL", "Unleashed", "t07", "Sprite (T07) // Buff")
        if c: extra_clones.append(c)
        
    mapped_cards.extend(extra_clones)
    print(f"Appended {len(extra_clones)} custom token/rune variations. Total cards: {len(mapped_cards)}")

def main():
    raw_cards = download_all_raw_cards()
    
    print(f"\nDownloading missing card images for {len(raw_cards)} cards concurrently...")
    success_count = 0
    already_exists_count = 0
    failed_count = 0
    
    with ThreadPoolExecutor(max_workers=30) as executor:
        future_to_card = {executor.submit(download_image, card): card for card in raw_cards}
        for future in as_completed(future_to_card):
            card_id, success, message = future.result()
            if success:
                if message == "Already exists":
                    already_exists_count += 1
                else:
                    success_count += 1
            else:
                failed_count += 1
                print(f"Failed image download for card {card_id}: {message}")
                
    print(f"Images summary: {already_exists_count} already cached, {success_count} newly downloaded, {failed_count} failed.")
    
    mapped_cards = build_mapped_cards(raw_cards)
    append_custom_tokens_and_runes(mapped_cards)
    
    with open(TARGET_JSON_PATH, "w", encoding="utf-8") as f:
        json.dump(mapped_cards, f, ensure_ascii=False, indent=2)
        
    print(f"\nUpdated {TARGET_JSON_PATH} with {len(mapped_cards)} total cards.")
    
    # Print summary per set
    set_counts = {}
    for c in mapped_cards:
        s = c["setCode"]
        set_counts[s] = set_counts.get(s, 0) + 1
        
    print("\nFinal card counts per set in app:")
    for s, cnt in sorted(set_counts.items()):
        print(f" - {s}: {cnt} cards")

if __name__ == "__main__":
    main()
