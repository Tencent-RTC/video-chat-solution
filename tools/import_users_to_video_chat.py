import time
import hmac
import hashlib
import base64
import json
import zlib
import random
import requests

# ── 配置 ────────────────────────────────────────────────────────────────────
SDKAPPID = 0
SECRETKEY = ""
EXPIRETIME = 604800
IDENTIFIER = "administrator"

BASE_URL = "https://console.tim.qq.com/v4/im_open_login_svc/multiaccount_import"
PROFILE_URL = "https://console.tim.qq.com/v4/profile/portrait_set"

# ── 用户数据 ──────────────────────────────────────────────────────────────────
USERS = [
    {
        "UserID": "VideoChatlinxiaoyu",
        "Nick": "林晓雨",
        "FaceUrl": "https://im.sdk.qcloud.com/download/tuikit-resource/avatar/avatar_7.png",
        "Gender": "Gender_Type_Female",
        "Location": "上海",
        "Signature": "用脚步丈量世界，用歌声记录生活🌍🎤",
        "Age": 23,
    },
    {
        "UserID": "VideoChatsu_menghan",
        "Nick": "苏梦涵",
        "FaceUrl": "https://im.sdk.qcloud.com/download/tuikit-resource/avatar/avatar_13.png",
        "Gender": "Gender_Type_Female",
        "Location": "北京",
        "Signature": "坚持健身，热爱跳舞，保持自律也期待浪漫的相遇✨",
        "Age": 21,
    },
    {
        "UserID": "VideoChatchenkexin",
        "Nick": "陈可欣",
        "FaceUrl": "https://im.sdk.qcloud.com/download/tuikit-resource/avatar/avatar_14.png",
        "Gender": "Gender_Type_Female",
        "Location": "广州",
        "Signature": "一半人间烟火，一半诗与画卷。很高兴认识你📖🎨",
        "Age": 25,
    },
    {
        "UserID": "VideoChatzhaoxinyi",
        "Nick": "赵欣怡",
        "FaceUrl": "https://im.sdk.qcloud.com/download/tuikit-resource/avatar/avatar_17.png",
        "Gender": "Gender_Type_Female",
        "Location": "成都",
        "Signature": "分享美妆与穿搭，做自己生活里的女主角💄👗",
        "Age": 22,
    },
    {
        "UserID": "VideoChatjiangsiqi",
        "Nick": "江思琪",
        "FaceUrl": "https://im.sdk.qcloud.com/download/tuikit-resource/avatar/avatar_19.png",
        "Gender": "Gender_Type_Female",
        "Location": "杭州",
        "Signature": "用镜头捕捉光影，用瑜伽感受呼吸。来和我分享你的故事吧📷🧘‍♀️",
        "Age": 24,
    },
    {
        "UserID": "VideoChatzhouyaqi",
        "Nick": "周雅琪",
        "FaceUrl": "https://im.sdk.qcloud.com/download/tuikit-resource/avatar/avatar_21.png",
        "Gender": "Gender_Type_Female",
        "Location": "深圳",
        "Signature": "耳机里是随机播放的浪漫，手里是一杯不加糖的拿铁☕️🎵",
        "Age": 23,
    },
]


# ── UserSig 生成 ────────────────────────────────────────────────────────────
def gen_user_sig(identifier: str) -> str:
    current = int(time.time())
    obj = {
        "TLS.ver": "2.0",
        "TLS.identifier": identifier,
        "TLS.sdkappid": SDKAPPID,
        "TLS.expire": EXPIRETIME,
        "TLS.time": current,
    }
    key_order = [
        "TLS.identifier",
        "TLS.sdkappid",
        "TLS.time",
        "TLS.expire",
    ]
    string_to_sign = ""
    for key in key_order:
        string_to_sign += f"{key}:{obj[key]}\n"

    sig = hmac_sha256(string_to_sign)
    obj["TLS.sig"] = sig

    json_data = json.dumps(obj, separators=(',', ':'), sort_keys=True).encode('utf-8')
    compressed = zlib.compress(json_data, level=zlib.Z_BEST_SPEED)
    return base64_url_encode(compressed)


def hmac_sha256(plain_text: str) -> str:
    secret_key = SECRETKEY.encode('ascii')
    message = plain_text.encode('ascii')
    signature = hmac.new(secret_key, message, digestmod=hashlib.sha256).digest()
    return base64.b64encode(signature).decode('utf-8')


def base64_url_encode(data: bytes) -> str:
    base64_str = base64.b64encode(data).decode('utf-8')
    return base64_str.replace('+', '*').replace('/', '-').replace('=', '_')


# ── 批量导入用户 ──────────────────────────────────────────────────────────────
def import_users(users: list) -> dict:
    user_sig = gen_user_sig(IDENTIFIER)
    rand = random.randint(0, 4294967295)

    url = (
        f"{BASE_URL}"
        f"?sdkappid={SDKAPPID}"
        f"&identifier={IDENTIFIER}"
        f"&usersig={user_sig}"
        f"&random={rand}"
        f"&contenttype=json"
    )

    payload = {"AccountList": users}
    response = requests.post(url, json=payload, timeout=10)
    return response.json()


# ── 设置用户资料 ──────────────────────────────────────────────────────────────
def build_profile_items(user: dict) -> list:
    """构建 portrait_set 需要的 ProfileItem 数组"""
    items = []

    # 标配资料字段
    def add(tag: str, value):
        if value:
            items.append({"Tag": tag, "Value": value})

    add("Tag_Profile_IM_Nick", user.get("Nick", ""))
    add("Tag_Profile_IM_Gender", user.get("Gender", ""))
    add("Tag_Profile_IM_SelfSignature", user.get("Signature", ""))

    # Tag_Profile_IM_BirthDay 是 uint32 类型，直接存年龄
    age = user.get("Age", 0)
    if age > 0:
        items.append({"Tag": "Tag_Profile_IM_BirthDay", "Value": age})

    return items


def set_portrait(user_id: str, profile_items: list) -> dict:
    """调用 profile/portrait_set 设置单个用户资料"""
    user_sig = gen_user_sig(IDENTIFIER)
    rand = random.randint(0, 4294967295)

    url = (
        f"{PROFILE_URL}"
        f"?sdkappid={SDKAPPID}"
        f"&identifier={IDENTIFIER}"
        f"&usersig={user_sig}"
        f"&random={rand}"
        f"&contenttype=json"
    )

    payload = {
        "From_Account": user_id,
        "ProfileItem": profile_items,
    }
    response = requests.post(url, json=payload, timeout=10)
    return response.json()


# ── 主流程 ────────────────────────────────────────────────────────────────────


def main():
    print(f"开始批量导入 {len(USERS)} 个用户...")
    print(f"SDKAppID: {SDKAPPID}")
    print(f"Identifier: {IDENTIFIER}")
    print("=" * 60)

    # 1. 批量导入用户
    result = import_users(USERS)

    if result.get("ErrorCode") != 0:
        print(f"\n❌ 导入失败: ErrorCode={result.get('ErrorCode')}, ErrorInfo={result.get('ErrorInfo')}")
        return

    fail_accounts = result.get("FailAccounts", [])
    if fail_accounts:
        print(f"\n⚠️  部分导入失败: {fail_accounts}")

    print(f"\n✅ 导入完成! 开始设置用户资料...")
    print("-" * 60)

    # 2. 逐个设置用户资料
    for user in USERS:
        user_id = user["UserID"]
        nick = user["Nick"]
        profile_items = build_profile_items(user)

        print(f"  设置 [{user_id}] ({nick}) ... ", end="", flush=True)
        result = set_portrait(user_id, profile_items)

        if result.get("ErrorCode") == 0:
            print("✅")
        else:
            print(f"❌ ErrorCode={result.get('ErrorCode')}, ErrorInfo={result.get('ErrorInfo')}")

    print("\n" + "=" * 60)
    print("导入的 UserID 列表（用于 MainActivity）:")
    for user in USERS:
        print(f'  "{user["UserID"]}",')


if __name__ == "__main__":
    main()
