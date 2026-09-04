#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
fake_rk3_server.py —— 模拟 RK3 设备局域网 HTTP 服务（答辩演示用假数据）

用途：
    真实 RK3 设备在老人家里开机自动启动 HTTP Server（端口 8080，无公网 IP），
    App 家属端报告页/日程情绪日卡通过设置页「RK3服务器地址」直连读取抑郁/焦虑预测。
    本脚本在本机模拟该设备，返回「抑郁预测很高」的假数据，用于答辩时演示
    「RK3 预测不佳 → 家属端报告页显示高风险」的真实链路效果。

用法：
    python fake_rk3_server.py            # 监听 0.0.0.0:8080
    python fake_rk3_server.py --port 8080

    App 家属端 → 我的 → 客服与设置 → RK3服务器地址 填 http://<本机局域网IP>:8080
    （手机与电脑需在同一 WiFi；本机 IP 可用 ipconfig 查看）

依赖：仅 Python 标准库（http.server），无需 pip install 任何包。

接口（与 App Rk3LanClient 解析约定完全一致）：
    GET /api/health                    → 实时状态 + 最近采集记录
    GET /api/reports/weekly?start=&end= → 周报（7 天）
    GET /api/reports/yearly?year=       → 年报（12 月）
    GET /api/suggestions/latest         → 最新家属建议
响应统一包裹 {"code": 200, "message": "success", "data": {...}}。
"""

import argparse
import json
import sys
import time
from datetime import date, timedelta
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

# ==================== 高风险抑郁数据常量（答辩演示值） ====================

# 本周七天每天的抑郁/焦虑百分比（模拟 RK3 预测结果不佳，均 ≥ 60%）
WEEK_DAYS = [
    ("周一", 82.0, 55.0),
    ("周二", 78.0, 52.0),
    ("周三", 85.0, 60.0),
    ("周四", 80.0, 53.0),
    ("周五", 76.0, 50.0),
    ("周六", 88.0, 62.0),
    ("周日", 83.0, 57.0),
]

# 全年 12 个月每月抑郁/焦虑均值
MONTHS = [
    (1, 45.0, 38.0), (2, 52.0, 40.0), (3, 60.0, 45.0),
    (4, 58.0, 44.0), (5, 63.0, 48.0), (6, 70.0, 52.0),
    (7, 75.0, 55.0), (8, 78.0, 58.0), (9, 82.0, 60.0),
    (10, 79.0, 56.0), (11, 84.0, 61.0), (12, 86.0, 63.0),
]

# 最近 5 条实时采集记录
RECENT_CAPTURES = [
    {"time": "14:32", "emotion": "抑郁", "depressionPercent": 86.0, "anxietyPercent": 62.0},
    {"time": "13:05", "emotion": "焦虑", "depressionPercent": 82.0, "anxietyPercent": 58.0},
    {"time": "11:48", "emotion": "抑郁", "depressionPercent": 79.0, "anxietyPercent": 55.0},
    {"time": "10:20", "emotion": "抑郁", "depressionPercent": 84.0, "anxietyPercent": 60.0},
    {"time": "09:02", "emotion": "平静", "depressionPercent": 61.0, "anxietyPercent": 45.0},
]


def _weekly_payload():
    """周报：7 天数据 + 汇总（周均值 = 高风险）。"""
    today = date.today()
    monday = today - timedelta(days=today.weekday())
    days = []
    total_dep = 0.0
    total_anx = 0.0
    peak_dep = 0.0
    peak_anx = 0.0
    for i, (_, dep, anx) in enumerate(WEEK_DAYS):
        d = (monday + timedelta(days=i)).strftime("%Y-%m-%d")
        days.append({
            "date": d,
            "avgDepressionPercent": dep,
            "avgAnxietyPercent": anx,
            "peakDepressionPercent": dep,
            "peakAnxietyPercent": anx,
            "captureCount": 8,
        })
        total_dep += dep
        total_anx += anx
        peak_dep = max(peak_dep, dep)
        peak_anx = max(peak_anx, anx)
    return {
        "avgDepressionPercent": round(total_dep / 7, 1),
        "avgAnxietyPercent": round(total_anx / 7, 1),
        "peakDepressionPercent": peak_dep,
        "peakAnxietyPercent": peak_anx,
        "totalCaptureCount": 56,
        "days": days,
    }


def _yearly_payload():
    """年报：12 个月 + 汇总 + 最高月份（近月抑郁最高）。"""
    months = []
    total_dep = 0.0
    total_anx = 0.0
    peak = (1, 0.0)
    for month, dep, anx in MONTHS:
        months.append({
            "month": month,
            "avgDepressionPercent": dep,
            "avgAnxietyPercent": anx,
        })
        total_dep += dep
        total_anx += anx
        if dep > peak[1]:
            peak = (month, dep)
    return {
        "avgDepressionPercent": round(total_dep / 12, 1),
        "avgAnxietyPercent": round(total_anx / 12, 1),
        "topMonth": f"{peak[0]}月",
        "months": months,
    }


def _suggestion_payload():
    """最新家属建议（触发等级 = 高风险）。"""
    return {
        "suggestionText": "近一周王建军的抑郁情绪指标持续偏高（均值82%），建议家属近期多陪伴交流、"
                          "陪同就医评估，并关注作息与睡眠情况。",
        "triggerLevelText": "高风险",
        "date": date.today().strftime("%Y-%m-%d"),
    }


def _health_payload():
    """实时状态 + 最近采集记录。"""
    return {
        "isDetecting": True,
        "detectStatus": "运行中",
        "frameCount": 128560,
        "faceCount": 2,
        "recentCaptures": RECENT_CAPTURES,
    }


ROUTES = {
    "/api/health": _health_payload,
    "/api/reports/weekly": _weekly_payload,
    "/api/reports/yearly": _yearly_payload,
    "/api/suggestions/latest": _suggestion_payload,
}


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path
        if path in ROUTES:
            data = ROUTES[path]()
            payload = json.dumps({"code": 200, "message": "success", "data": data},
                                 ensure_ascii=False).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Content-Length", str(len(payload)))
            self.send_header("Access-Control-Allow-Origin", "*")
            self.end_headers()
            self.wfile.write(payload)
            print(f"[RK3-FAKE] 200 {path} <- {self.client_address[0]}")
        else:
            self.send_response(404)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            body = json.dumps({"code": 404, "message": "not found"}, ensure_ascii=False).encode("utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            print(f"[RK3-FAKE] 404 {path}")

    def log_message(self, fmt, *args):
        # 静默默认访问日志，避免刷屏
        pass


def main():
    parser = argparse.ArgumentParser(description="模拟 RK3 设备局域网 HTTP 服务（高抑郁假数据）")
    parser.add_argument("--port", type=int, default=8080, help="监听端口（默认 8080，与真实 RK3 一致）")
    parser.add_argument("--host", type=str, default="0.0.0.0", help="监听地址（默认 0.0.0.0）")
    args = parser.parse_args()

    print("=" * 56)
    print("  假 RK3 设备服务已启动（答辩演示 · 高抑郁假数据）")
    print(f"  监听: {args.host}:{args.port}")
    print("  接口: /api/health | /api/reports/weekly | /api/reports/yearly | /api/suggestions/latest")
    print("  提示: App 家属端设置页填 http://<本机局域网IP>:8080 即可连通")
    print("=" * 56)
    try:
        ThreadingHTTPServer((args.host, args.port), Handler).serve_forever()
    except KeyboardInterrupt:
        print("\n已停止。")
        sys.exit(0)


if __name__ == "__main__":
    main()
