"""
生成 SPU + SKU + 库存种子数据的 SQL 脚本
用途：30 个 SPU，每个至少 4 个 SKU
"""
import random
from datetime import datetime, timezone

random.seed(42)

# ============================================================
# 数据定义
# ============================================================

# SPU 模板：(spu_name, category_id, brand_id, 属性键列表, [SKU 规格组合])
# 每个 SKU 组合: (规格值 dict, 价格偏移量(分))
SPUS = [
    # --- 智能手机 (category=3) ---
    {
        "name": "iPhone 16 Pro Max",
        "cat": 3, "brand": 1, "desc": "Apple A18 Pro 芯片，6.9 英寸超视网膜 XDR 显示屏，钛金属设计，4800 万像素主摄",
        "image": "https://cdn.example.com/spu/2001/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "沙漠色钛金属", "存储": "256GB"}, 0),
            ({"颜色": "沙漠色钛金属", "存储": "512GB"}, 150000),
            ({"颜色": "沙漠色钛金属", "存储": "1TB"},   300000),
            ({"颜色": "原色钛金属",   "存储": "256GB"}, 0),
            ({"颜色": "原色钛金属",   "存储": "512GB"}, 150000),
            ({"颜色": "白色钛金属",   "存储": "256GB"}, 0),
        ],
        "base_price": 999900,
        "market_diff": 100000,
        "cost_pct": 0.65,
        "weight": 227,
    },
    {
        "name": "iPhone 16 Pro",
        "cat": 3, "brand": 1, "desc": "Apple A18 Pro 芯片，6.3 英寸超视网膜 XDR 显示屏，钛金属设计",
        "image": "https://cdn.example.com/spu/2002/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "沙漠色钛金属", "存储": "128GB"}, 0),
            ({"颜色": "沙漠色钛金属", "存储": "256GB"}, 100000),
            ({"颜色": "沙漠色钛金属", "存储": "512GB"}, 250000),
            ({"颜色": "原色钛金属",   "存储": "128GB"}, 0),
            ({"颜色": "原色钛金属",   "存储": "256GB"}, 100000),
        ],
        "base_price": 799900,
        "market_diff": 80000,
        "cost_pct": 0.65,
        "weight": 199,
    },
    {
        "name": "iPhone 16",
        "cat": 3, "brand": 1, "desc": "Apple A18 芯片，6.1 英寸超视网膜 XDR 显示屏，4800 万像素主摄",
        "image": "https://cdn.example.com/spu/2003/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "群青色", "存储": "128GB"}, 0),
            ({"颜色": "群青色", "存储": "256GB"}, 100000),
            ({"颜色": "深青色", "存储": "128GB"}, 0),
            ({"颜色": "深青色", "存储": "256GB"}, 100000),
            ({"颜色": "粉色",   "存储": "128GB"}, 0),
        ],
        "base_price": 599900,
        "market_diff": 60000,
        "cost_pct": 0.65,
        "weight": 170,
    },
    {
        "name": "华为 Mate 70 Pro",
        "cat": 3, "brand": 2, "desc": "麒麟 9100 芯片，6.9 英寸 OLED 屏幕，5000 万像素超聚光主摄，卫星通信",
        "image": "https://cdn.example.com/spu/2004/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "曜石黑", "存储": "256GB"}, 0),
            ({"颜色": "曜石黑", "存储": "512GB"}, 100000),
            ({"颜色": "雪域白", "存储": "256GB"}, 0),
            ({"颜色": "雪域白", "存储": "1TB"},   250000),
            ({"颜色": "罗兰紫", "存储": "512GB"}, 100000),
        ],
        "base_price": 699900,
        "market_diff": 70000,
        "cost_pct": 0.60,
        "weight": 210,
    },
    {
        "name": "华为 Pura 80 Pro",
        "cat": 3, "brand": 2, "desc": "麒麟 9000 芯片，6.7 英寸 OLED 四曲屏，XMAGE 影像系统",
        "image": "https://cdn.example.com/spu/2005/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "罗兰紫", "存储": "256GB"}, 0),
            ({"颜色": "罗兰紫", "存储": "512GB"}, 120000),
            ({"颜色": "雪域白", "存储": "256GB"}, 0),
            ({"颜色": "雪域白", "存储": "1TB"},   280000),
            ({"颜色": "深海蓝", "存储": "512GB"}, 120000),
        ],
        "base_price": 599900,
        "market_diff": 60000,
        "cost_pct": 0.60,
        "weight": 195,
    },
    {
        "name": "华为 Mate X6",
        "cat": 3, "brand": 2, "desc": "折叠屏旗舰，麒麟 9100 芯片，7.85 英寸柔性内屏，5000 万像素主摄",
        "image": "https://cdn.example.com/spu/2006/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "曜石黑", "存储": "256GB"}, 0),
            ({"颜色": "曜石黑", "存储": "512GB"}, 100000),
            ({"颜色": "雪域白", "存储": "512GB"}, 100000),
            ({"颜色": "寰宇红", "存储": "1TB"},   200000),
        ],
        "base_price": 1399900,
        "market_diff": 150000,
        "cost_pct": 0.55,
        "weight": 243,
    },
    {
        "name": "iPhone 15 Pro Max",
        "cat": 3, "brand": 1, "desc": "Apple A17 Pro 芯片，6.7 英寸超视网膜 XDR 显示屏，钛金属设计",
        "image": "https://cdn.example.com/spu/2007/main.jpg",
        "attrs": ["颜色", "存储"],
        "skus": [
            ({"颜色": "原色钛金属", "存储": "256GB"}, 0),
            ({"颜色": "原色钛金属", "存储": "512GB"}, 150000),
            ({"颜色": "蓝色钛金属", "存储": "256GB"}, 0),
            ({"颜色": "蓝色钛金属", "存储": "512GB"}, 150000),
            ({"颜色": "白色钛金属", "存储": "256GB"}, 0),
        ],
        "base_price": 799900,
        "market_diff": 100000,
        "cost_pct": 0.60,
        "weight": 221,
    },
    # --- 笔记本 (category=8) ---
    {
        "name": "MacBook Pro 16",
        "cat": 8, "brand": 1, "desc": "Apple M4 Max 芯片，16.2 英寸 Liquid Retina XDR 显示屏，36GB 统一内存",
        "image": "https://cdn.example.com/spu/2008/main.jpg",
        "attrs": ["颜色", "配置"],
        "skus": [
            ({"颜色": "深空黑", "配置": "M4 Max/36GB/512GB"}, 0),
            ({"颜色": "深空黑", "配置": "M4 Max/48GB/1TB"},   300000),
            ({"颜色": "银色",   "配置": "M4 Max/36GB/512GB"}, 0),
            ({"颜色": "银色",   "配置": "M4 Max/48GB/1TB"},   300000),
        ],
        "base_price": 1999900,
        "market_diff": 200000,
        "cost_pct": 0.60,
        "weight": 2140,
    },
    {
        "name": "MacBook Air 15",
        "cat": 8, "brand": 1, "desc": "Apple M4 芯片，15.3 英寸 Liquid Retina 显示屏，18GB 统一内存",
        "image": "https://cdn.example.com/spu/2009/main.jpg",
        "attrs": ["颜色", "配置"],
        "skus": [
            ({"颜色": "午夜黑", "配置": "M4/18GB/256GB"}, 0),
            ({"颜色": "午夜黑", "配置": "M4/24GB/512GB"}, 200000),
            ({"颜色": "星光色", "配置": "M4/18GB/256GB"}, 0),
            ({"颜色": "星光色", "配置": "M4/24GB/512GB"}, 200000),
            ({"颜色": "银色",   "配置": "M4/18GB/256GB"}, 0),
        ],
        "base_price": 1099900,
        "market_diff": 100000,
        "cost_pct": 0.60,
        "weight": 1510,
    },
    {
        "name": "华为 MateBook X Pro",
        "cat": 8, "brand": 2, "desc": "Intel Core Ultra 9，14.2 英寸 OLED 3K 触控屏，32GB 内存，超级终端",
        "image": "https://cdn.example.com/spu/2010/main.jpg",
        "attrs": ["颜色", "配置"],
        "skus": [
            ({"颜色": "皓月银", "配置": "Ultra 9/32GB/1TB"}, 0),
            ({"颜色": "皓月银", "配置": "Ultra 9/32GB/2TB"}, 200000),
            ({"颜色": "深空灰", "配置": "Ultra 9/32GB/1TB"}, 0),
            ({"颜色": "深空灰", "配置": "Ultra 9/32GB/2TB"}, 200000),
        ],
        "base_price": 1299900,
        "market_diff": 130000,
        "cost_pct": 0.55,
        "weight": 1290,
    },
    {
        "name": "华为 MateBook 14",
        "cat": 8, "brand": 2, "desc": "Intel Core Ultra 5，14 英寸 2K 触控屏，16GB 内存，超级终端",
        "image": "https://cdn.example.com/spu/2011/main.jpg",
        "attrs": ["颜色", "配置"],
        "skus": [
            ({"颜色": "皓月银", "配置": "Ultra 5/16GB/512GB"}, 0),
            ({"颜色": "皓月银", "配置": "Ultra 5/16GB/1TB"},  100000),
            ({"颜色": "深空灰", "配置": "Ultra 5/16GB/512GB"}, 0),
            ({"颜色": "深空灰", "配置": "Ultra 5/16GB/1TB"},  100000),
            ({"颜色": "樱语粉", "配置": "Ultra 5/16GB/512GB"}, 0),
        ],
        "base_price": 699900,
        "market_diff": 70000,
        "cost_pct": 0.55,
        "weight": 1380,
    },
    {
        "name": "MacBook Pro 14",
        "cat": 8, "brand": 1, "desc": "Apple M4 Pro 芯片，14.2 英寸 Liquid Retina XDR 显示屏，24GB 统一内存",
        "image": "https://cdn.example.com/spu/2012/main.jpg",
        "attrs": ["颜色", "配置"],
        "skus": [
            ({"颜色": "深空黑", "配置": "M4 Pro/24GB/512GB"}, 0),
            ({"颜色": "深空黑", "配置": "M4 Pro/24GB/1TB"},  200000),
            ({"颜色": "银色",   "配置": "M4 Pro/24GB/512GB"}, 0),
            ({"颜色": "银色",   "配置": "M4 Pro/24GB/1TB"},  200000),
        ],
        "base_price": 1499900,
        "market_diff": 150000,
        "cost_pct": 0.60,
        "weight": 1610,
    },
    {
        "name": "华为 MateBook 16s",
        "cat": 8, "brand": 2, "desc": "Intel Core Ultra 7，16 英寸 2.5K 大屏，32GB 内存，数字小键盘",
        "image": "https://cdn.example.com/spu/2013/main.jpg",
        "attrs": ["颜色", "配置"],
        "skus": [
            ({"颜色": "深空灰", "配置": "Ultra 7/32GB/1TB"}, 0),
            ({"颜色": "深空灰", "配置": "Ultra 7/32GB/2TB"}, 150000),
            ({"颜色": "皓月银", "配置": "Ultra 7/32GB/1TB"}, 0),
            ({"颜色": "皓月银", "配置": "Ultra 7/32GB/2TB"}, 150000),
        ],
        "base_price": 899900,
        "market_diff": 90000,
        "cost_pct": 0.55,
        "weight": 1990,
    },
    # --- 空调 (category=13) ---
    {
        "name": "1.5匹 新一级变频冷暖空调",
        "cat": 13, "brand": 2, "desc": "新一级能效，变频冷暖，自清洁，智能WiFi控制，适用15-22㎡",
        "image": "https://cdn.example.com/spu/2014/main.jpg",
        "attrs": ["颜色", "功能"],
        "skus": [
            ({"颜色": "白色", "功能": "标准版"},      0),
            ({"颜色": "白色", "功能": "Pro版(除甲醛)"}, 30000),
            ({"颜色": "白色", "功能": "旗舰版(新风)"},  50000),
            ({"颜色": "米色", "功能": "标准版"},      0),
        ],
        "base_price": 299900,
        "market_diff": 30000,
        "cost_pct": 0.55,
        "weight": 10000,
    },
    {
        "name": "3匹 新一级变频冷暖立式空调",
        "cat": 13, "brand": 2, "desc": "新一级能效，变频冷暖，圆柱柜机，自清洁，适用30-45㎡",
        "image": "https://cdn.example.com/spu/2015/main.jpg",
        "attrs": ["颜色", "功能"],
        "skus": [
            ({"颜色": "白色", "功能": "标准版"},      0),
            ({"颜色": "白色", "功能": "Pro版(除甲醛)"}, 40000),
            ({"颜色": "白色", "功能": "旗舰版(新风)"},  70000),
            ({"颜色": "灰色", "功能": "标准版"},      0),
            ({"颜色": "灰色", "功能": "旗舰版(新风)"},  70000),
        ],
        "base_price": 599900,
        "market_diff": 60000,
        "cost_pct": 0.55,
        "weight": 28000,
    },
    {
        "name": "大1匹 变频冷暖空调",
        "cat": 13, "brand": 2, "desc": "新三级能效，变频冷暖，静音设计，适用10-15㎡",
        "image": "https://cdn.example.com/spu/2016/main.jpg",
        "attrs": ["颜色", "功能"],
        "skus": [
            ({"颜色": "白色", "功能": "标准版"},     0),
            ({"颜色": "白色", "功能": "静音版"},      15000),
            ({"颜色": "白色", "功能": "除湿版"},      10000),
            ({"颜色": "米色", "功能": "标准版"},     0),
        ],
        "base_price": 259900,
        "market_diff": 26000,
        "cost_pct": 0.55,
        "weight": 9000,
    },
    {
        "name": "2匹 变频冷暖挂式空调",
        "cat": 13, "brand": 2, "desc": "新一级能效，变频冷暖，挂机设计，自清洁，适用20-30㎡",
        "image": "https://cdn.example.com/spu/2017/main.jpg",
        "attrs": ["颜色", "功能"],
        "skus": [
            ({"颜色": "白色", "功能": "标准版"},      0),
            ({"颜色": "白色", "功能": "Pro版(除甲醛)"}, 35000),
            ({"颜色": "白色", "功能": "旗舰版(新风)"},  55000),
            ({"颜色": "灰色", "功能": "标准版"},      0),
        ],
        "base_price": 449900,
        "market_diff": 45000,
        "cost_pct": 0.55,
        "weight": 15000,
    },
    {
        "name": "中央空调 一拖四 多联机",
        "cat": 13, "brand": 2, "desc": "全直流变频，一级能效，一拖四，智能控制，适用80-110㎡",
        "image": "https://cdn.example.com/spu/2018/main.jpg",
        "attrs": ["颜色", "功能"],
        "skus": [
            ({"颜色": "白色", "功能": "标准版(4匹)"},   0),
            ({"颜色": "白色", "功能": "Pro版(5匹)"},    100000),
            ({"颜色": "白色", "功能": "旗舰版(6匹+3D)"}, 200000),
            ({"颜色": "灰色", "功能": "标准版(4匹)"},   0),
        ],
        "base_price": 1299900,
        "market_diff": 130000,
        "cost_pct": 0.50,
        "weight": 52000,
    },
    # --- 上衣 (category=18) ---
    {
        "name": "Nike Dri-FIT 女子速干运动T恤",
        "cat": 18, "brand": 3, "desc": "Dri-FIT 科技排汗速干，轻盈透气，宽松版型，运动健身必备",
        "image": "https://cdn.example.com/spu/2019/main.jpg",
        "attrs": ["颜色", "尺码"],
        "skus": [
            ({"颜色": "黑色", "尺码": "S"},  0),
            ({"颜色": "黑色", "尺码": "M"},  0),
            ({"颜色": "黑色", "尺码": "L"},  0),
            ({"颜色": "黑色", "尺码": "XL"}, 0),
            ({"颜色": "白色", "尺码": "M"},  0),
            ({"颜色": "白色", "尺码": "L"},  0),
        ],
        "base_price": 29900,
        "market_diff": 3000,
        "cost_pct": 0.40,
        "weight": 150,
    },
    {
        "name": "Nike 男子套头卫衣",
        "cat": 18, "brand": 3, "desc": "经典棉混纺面料，舒适保暖，简约Logo印花，日常休闲百搭",
        "image": "https://cdn.example.com/spu/2020/main.jpg",
        "attrs": ["颜色", "尺码"],
        "skus": [
            ({"颜色": "灰色", "尺码": "M"},  0),
            ({"颜色": "灰色", "尺码": "L"},  0),
            ({"颜色": "灰色", "尺码": "XL"}, 0),
            ({"颜色": "黑色", "尺码": "M"},  0),
            ({"颜色": "黑色", "尺码": "L"},  0),
            ({"颜色": "黑色", "尺码": "XL"}, 0),
        ],
        "base_price": 49900,
        "market_diff": 5000,
        "cost_pct": 0.40,
        "weight": 350,
    },
    {
        "name": "Nike 女子瑜伽运动上衣",
        "cat": 18, "brand": 3, "desc": "高弹力面料，速干透气，支撑性设计，适合瑜伽、普拉提等低强度运动",
        "image": "https://cdn.example.com/spu/2021/main.jpg",
        "attrs": ["颜色", "尺码"],
        "skus": [
            ({"颜色": "紫色", "尺码": "S"},  0),
            ({"颜色": "紫色", "尺码": "M"},  0),
            ({"颜色": "紫色", "尺码": "L"},  0),
            ({"颜色": "粉色", "尺码": "S"},  0),
            ({"颜色": "粉色", "尺码": "M"},  0),
        ],
        "base_price": 39900,
        "market_diff": 4000,
        "cost_pct": 0.35,
        "weight": 120,
    },
    {
        "name": "Nike 男子连帽夹克",
        "cat": 18, "brand": 3, "desc": "防泼水面料，连帽设计，多口袋储物，春秋季户外休闲风衣",
        "image": "https://cdn.example.com/spu/2022/main.jpg",
        "attrs": ["颜色", "尺码"],
        "skus": [
            ({"颜色": "黑色", "尺码": "M"},  0),
            ({"颜色": "黑色", "尺码": "L"},  0),
            ({"颜色": "黑色", "尺码": "XL"}, 0),
            ({"颜色": "黑色", "尺码": "XXL"}, 0),
            ({"颜色": "深蓝色", "尺码": "L"},  0),
            ({"颜色": "深蓝色", "尺码": "XL"}, 0),
        ],
        "base_price": 89900,
        "market_diff": 9000,
        "cost_pct": 0.40,
        "weight": 450,
    },
    {
        "name": "Nike 女子运动背心",
        "cat": 18, "brand": 3, "desc": "轻盈透气面料，工字背设计，内置胸垫，适合跑步、健身等高强度运动",
        "image": "https://cdn.example.com/spu/2023/main.jpg",
        "attrs": ["颜色", "尺码"],
        "skus": [
            ({"颜色": "黑色", "尺码": "S"},  0),
            ({"颜色": "黑色", "尺码": "M"},  0),
            ({"颜色": "黑色", "尺码": "L"},  0),
            ({"颜色": "白色", "尺码": "S"},  0),
            ({"颜色": "白色", "尺码": "M"},  0),
        ],
        "base_price": 24900,
        "market_diff": 2500,
        "cost_pct": 0.35,
        "weight": 80,
    },
    {
        "name": "Nike 男子休闲Polo衫",
        "cat": 18, "brand": 3, "desc": "珠地棉面料，经典翻领设计，Logo刺绣，商务休闲两穿",
        "image": "https://cdn.example.com/spu/2024/main.jpg",
        "attrs": ["颜色", "尺码"],
        "skus": [
            ({"颜色": "白色", "尺码": "M"},  0),
            ({"颜色": "白色", "尺码": "L"},  0),
            ({"颜色": "白色", "尺码": "XL"}, 0),
            ({"颜色": "黑色", "尺码": "M"},  0),
            ({"颜色": "黑色", "尺码": "L"},  0),
            ({"颜色": "黑色", "尺码": "XL"}, 0),
        ],
        "base_price": 59900,
        "market_diff": 6000,
        "cost_pct": 0.40,
        "weight": 200,
    },
    # --- 面膜 (category=26) ---
    {
        "name": "玻尿酸补水保湿面膜 20片装",
        "cat": 26, "brand": None,
        "desc": "三重玻尿酸精华，深层补水保湿，修护肌肤屏障，适合所有肤质",
        "image": "https://cdn.example.com/spu/2025/main.jpg",
        "attrs": ["规格", "包装"],
        "skus": [
            ({"规格": "20片", "包装": "盒装"},       0),
            ({"规格": "20片", "包装": "礼盒装(含赠品)"}, 3000),
            ({"规格": "10片", "包装": "盒装"},       0),
            ({"规格": "10片", "包装": "试用装"},      -2000),
        ],
        "base_price": 12900,
        "market_diff": 1300,
        "cost_pct": 0.30,
        "weight": 280,
    },
    {
        "name": "烟酰胺美白淡斑面膜 10片装",
        "cat": 26, "brand": None,
        "desc": "5%烟酰胺精华，淡化暗沉提亮肤色，改善痘印色斑",
        "image": "https://cdn.example.com/spu/2026/main.jpg",
        "attrs": ["规格", "包装"],
        "skus": [
            ({"规格": "10片", "包装": "盒装"},       0),
            ({"规格": "10片", "包装": "礼盒装(含赠品)"}, 3000),
            ({"规格": "5片",  "包装": "盒装"},        0),
            ({"规格": "5片",  "包装": "试用装"},      -1500),
        ],
        "base_price": 9900,
        "market_diff": 1000,
        "cost_pct": 0.30,
        "weight": 150,
    },
    {
        "name": "积雪草舒缓修护面膜 10片装",
        "cat": 26, "brand": None,
        "desc": "积雪草提取物，舒缓敏感泛红，修护肌肤屏障，敏感肌适用",
        "image": "https://cdn.example.com/spu/2027/main.jpg",
        "attrs": ["规格", "包装"],
        "skus": [
            ({"规格": "10片", "包装": "盒装"},       0),
            ({"规格": "10片", "包装": "礼盒装(含赠品)"}, 3000),
            ({"规格": "5片",  "包装": "盒装"},        0),
            ({"规格": "5片",  "包装": "试用装"},      -1500),
            ({"规格": "20片", "包装": "盒装"},        5000),
        ],
        "base_price": 11900,
        "market_diff": 1200,
        "cost_pct": 0.30,
        "weight": 160,
    },
    {
        "name": "水杨酸控油祛痘面膜 10片装",
        "cat": 26, "brand": None,
        "desc": "2%水杨酸精华，控油祛痘，清洁毛孔，改善闭口粉刺",
        "image": "https://cdn.example.com/spu/2028/main.jpg",
        "attrs": ["规格", "包装"],
        "skus": [
            ({"规格": "10片", "包装": "盒装"},       0),
            ({"规格": "10片", "包装": "礼盒装(含赠品)"}, 3000),
            ({"规格": "5片",  "包装": "盒装"},        0),
            ({"规格": "5片",  "包装": "试用装"},      -1500),
        ],
        "base_price": 10900,
        "market_diff": 1100,
        "cost_pct": 0.30,
        "weight": 155,
    },
    {
        "name": "胶原蛋白紧致弹润面膜 10片装",
        "cat": 26, "brand": None,
        "desc": "重组胶原蛋白精华，紧致提拉，淡化细纹，抗氧化抗衰老",
        "image": "https://cdn.example.com/spu/2029/main.jpg",
        "attrs": ["规格", "包装"],
        "skus": [
            ({"规格": "10片", "包装": "盒装"},       0),
            ({"规格": "10片", "包装": "礼盒装(含赠品)"}, 3000),
            ({"规格": "5片",  "包装": "盒装"},        0),
            ({"规格": "20片", "包装": "盒装"},        7000),
        ],
        "base_price": 16900,
        "market_diff": 1700,
        "cost_pct": 0.30,
        "weight": 180,
    },
    {
        "name": "维C提亮抗氧面膜 10片装",
        "cat": 26, "brand": None,
        "desc": "VC衍生物精华，抗氧提亮，改善暗沉，焕活肌肤光泽",
        "image": "https://cdn.example.com/spu/2030/main.jpg",
        "attrs": ["规格", "包装"],
        "skus": [
            ({"规格": "10片", "包装": "盒装"},       0),
            ({"规格": "10片", "包装": "礼盒装(含赠品)"}, 3000),
            ({"规格": "5片",  "包装": "盒装"},        0),
            ({"规格": "5片",  "包装": "试用装"},      -1500),
            ({"规格": "20片", "包装": "盒装"},        6000),
        ],
        "base_price": 13900,
        "market_diff": 1400,
        "cost_pct": 0.30,
        "weight": 170,
    },
]

# ============================================================
# 生成 SQL
# ============================================================

now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S")
admin = "system"

spu_lines = []
sku_lines = []
stock_lines = []
spu_id_seq = 2001
sku_id_seq = 200101

for i, spu in enumerate(SPUS):
    spu_id = spu_id_seq + i
    base_price = spu["base_price"]
    market_diff = spu["market_diff"]
    cost_pct = spu["cost_pct"]

    # 计算 SPU 的 price_min/price_max
    prices = [base_price + offset for _, offset in spu["skus"]]
    price_min = min(prices)
    price_max = max(prices)

    brand_str = str(spu["brand"]) if spu["brand"] is not None else "NULL"
    sku_count = len(spu["skus"])

    spu_lines.append(
        f"({spu_id}, {spu['cat']}, {brand_str}, '{spu['name']}',"
        f" '<p>{spu['desc']}</p>',"
        f" '{spu['image']}',"
        f" '[\"{spu['image']}\",\"{spu['image'].replace('/main.', '/sub1.')}\",\"{spu['image'].replace('/main.', '/sub2.')}\"]',"
        f" {price_min}, {price_max},"
        f" {random.randint(100, 5000)}, {random.randint(10, 500)},"
        f" 1, 1, 0,"
        f" '{admin}', '{admin}',"
        f" '{now}', '{now}', 0)"
    )

    for j, (attrs, offset) in enumerate(spu["skus"]):
        sku_id = spu_id * 100 + (j + 1)

        # SKU 编码
        brand_abbr = {1: "AP", 2: "HW", 3: "NK"}.get(spu["brand"], "GN")
        idx_str = str(j + 1)
        sku_code = f"{brand_abbr}-{spu_id}-{idx_str}".upper()

        sku_price = base_price + offset
        market_price = sku_price + market_diff
        cost_price = int(sku_price * cost_pct)

        # SKU 名称
        color = attrs[list(attrs.keys())[0]]
        spec = attrs[list(attrs.keys())[1]]
        # 品牌前缀
        brand_name = {1: "Apple", 2: "华为", 3: "Nike"}.get(spu["brand"], "")
        sku_name = f"{brand_name} {spu['name'].replace(brand_name + ' ', '')} {color} {spec}"

        # attrs_json
        attrs_json = "[" + ",".join(f'{{"k":"{k}","v":"{v}"}}' for k, v in attrs.items()) + "]"

        weight = spu["weight"]

        sku_lines.append(
            f"({sku_id}, {spu_id}, '{sku_code}', '{sku_name}',"
            f" '{attrs_json}',"
            f" {sku_price}, {market_price}, {cost_price},"
            f" 'https://cdn.example.com/sku/{sku_id}.jpg',"
            f" {weight},"
            f" {random.randint(50, 2000)}, 0,"
            f" '{now}', '{now}')"
        )

        # 库存
        total = random.randint(200, 2000)
        sold = random.randint(0, int(total * 0.6))
        avail = total - sold
        locked = random.randint(0, min(50, avail))
        frozen = random.randint(0, min(20, avail - locked))
        avail -= locked + frozen

        stock_lines.append(
            f"({sku_id}, {total}, {avail}, {locked}, {sold}, {frozen}, 0, '{now}', '{now}', 0)"
        )

# ============================================================
# 输出 SQL
# ============================================================

sql = f"""-- ============================================
-- 商品 SPU + SKU + 库存 种子数据
-- 生成时间：{now}
-- 包含：30 个 SPU，每个至少 4 个 SKU
-- ============================================

-- ----------------------------
-- 清空旧数据（按约束顺序）
-- ----------------------------
DELETE FROM `mall_product_sku_stock` WHERE `sku_id` >= 200101;
DELETE FROM `mall_product_sku` WHERE `spu_id` >= 2001;
DELETE FROM `mall_product_spu` WHERE `id` >= 2001;

-- ----------------------------
-- 1. SPU 数据（30 条）
-- ----------------------------
INSERT INTO `mall_product_spu` (
    `id`, `category_id`, `brand_id`, `spu_name`, `spu_description`,
    `main_image`, `images_json`,
    `price_min`, `price_max`,
    `sales_count`, `review_count`,
    `publish_status`, `verify_status`, `is_deleted`,
    `create_by`, `update_by`,
    `create_time`, `update_time`, `version`
) VALUES
""" + ",\n".join(spu_lines) + """;
SELECT 'SPU数据导入完成' AS `status`;

-- ----------------------------
-- 2. SKU 数据（至少 4 个/SPU）
-- ----------------------------
INSERT INTO `mall_product_sku` (
    `id`, `spu_id`, `sku_code`, `sku_name`, `attrs_json`,
    `price`, `market_price`, `cost_price`, `image`, `weight`,
    `sales_count`, `is_deleted`,
    `create_time`, `update_time`
) VALUES
""" + ",\n".join(sku_lines) + """;
SELECT 'SKU数据导入完成' AS `status`;

-- ----------------------------
-- 3. SKU 库存数据
-- ----------------------------
INSERT INTO `mall_product_sku_stock` (
    `sku_id`, `total_stock`, `available_stock`, `locked_stock`,
    `sold_stock`, `frozen_stock`, `is_deleted`,
    `create_time`, `update_time`, `version`
) VALUES
""" + ",\n".join(stock_lines) + """;
SELECT '库存数据导入完成' AS `status`;"""

output_path = "db/mall-sql/V1.0.3__seed_mall_product_spu_sku.sql"
with open(output_path, "w", encoding="utf-8") as f:
    f.write(sql)
print(f"SQL 已写入 {output_path}")
print(f"共生成 {len(SPUS)} 个 SPU，{len(sku_lines)} 个 SKU，{len(stock_lines)} 条库存记录")
