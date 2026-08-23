package com.elderlycare.app.data.ezviz

import com.elderlycare.app.data.ezviz.model.AudioCategory
import com.elderlycare.app.data.ezviz.model.AudioTrack
import com.elderlycare.app.data.ezviz.model.FmGroup
import com.elderlycare.app.data.ezviz.model.FmStation

/**
 * RK3 点播/FM Mock 数据源（演示答辩用）。
 *
 * Mock 开关（设置页/页面 TopBar Switch）打开时，页面展示本数据并在点击卡片时
 * 本地模拟播放状态切换（不发起任何网络请求）；真实接口经萤石商务开通权限后，
 * 由 Rk3MediaRepository 网络数据替换本数据源。
 *
 * 内容为演示占位（标题仿萤石云视频 App 内容库风格），contentId/fmId 为占位值，
 * 待萤石内部文档确认真实 ID 体系。
 */
object Rk3MediaMock {

    val audioTracks: Map<AudioCategory, List<AudioTrack>> = mapOf(
        AudioCategory.RECOMMEND to listOf(
            AudioTrack("rec-001", "宝宝睡前故事精选", "童话故事 · 精选合集", 540),
            AudioTrack("rec-002", "国粹戏曲名段欣赏", "戏曲 · 名家唱段", 720),
            AudioTrack("rec-003", "唐诗三百首跟读", "诗词跟学 · 小学必背", 480),
            AudioTrack("rec-004", "世界经典童话大全", "童话故事 · 格林童话", premium = true),
            AudioTrack("rec-005", "古典音乐助眠曲", "音乐 · 轻音乐", 900, premium = true),
            AudioTrack("rec-006", "儿童睡前轻音乐", "音乐 · 摇篮曲", 360)
        ),
        AudioCategory.MUSIC to listOf(
            AudioTrack("mus-001", "小星星变奏曲", "莫扎特 · 钢琴曲", 330),
            AudioTrack("mus-002", "月光奏鸣曲", "贝多芬 · 钢琴曲", 420),
            AudioTrack("mus-003", "致爱丽丝", "贝多芬 · 钢琴曲", 180),
            AudioTrack("mus-004", "茉莉花", "民乐 · 经典", 240),
            AudioTrack("mus-005", "二泉映月", "阿炳 · 二胡", 390),
            AudioTrack("mus-006", "梁祝（小提琴协奏曲）", "何占豪/陈钢 · 小提琴", 510, premium = true),
            AudioTrack("mus-007", "天空之城", "久石让 · 轻音乐", 270),
            AudioTrack("mus-008", "卡农", "帕赫贝尔 · 钢琴曲", 300)
        ),
        AudioCategory.OPERA to listOf(
            AudioTrack("op-001", "贵妃醉酒（海岛冰轮初转腾）", "京剧 · 梅兰芳", 480),
            AudioTrack("op-002", "智取威虎山·打虎上山", "京剧 · 现代戏", 360),
            AudioTrack("op-003", "女驸马·谁料皇榜中状元", "黄梅戏 · 经典", 300),
            AudioTrack("op-004", "天仙配·夫妻双双把家还", "黄梅戏 · 经典", 240),
            AudioTrack("op-005", "牡丹亭·游园惊梦", "昆曲 · 经典", 540, premium = true),
            AudioTrack("op-006", "沙家浜·智斗", "京剧 · 现代戏", 420)
        ),
        AudioCategory.FAIRY_TALE to listOf(
            AudioTrack("ft-001", "白雪公主", "格林童话 · 经典", 600),
            AudioTrack("ft-002", "灰姑娘", "格林童话 · 经典", 540),
            AudioTrack("ft-003", "小红帽", "格林童话 · 经典", 300),
            AudioTrack("ft-004", "丑小鸭", "安徒生童话 · 经典", 480),
            AudioTrack("ft-005", "三只小猪", "经典寓言 · 启蒙", 240),
            AudioTrack("ft-006", "龟兔赛跑", "伊索寓言 · 启蒙", 180),
            AudioTrack("ft-007", "皇帝的新装", "安徒生童话 · 经典", 420, premium = true),
            AudioTrack("ft-008", "卖火柴的小女孩", "安徒生童话 · 经典", 360)
        ),
        AudioCategory.POETRY to listOf(
            AudioTrack("po-001", "静夜思", "李白 · 小学必背", 90),
            AudioTrack("po-002", "春晓", "孟浩然 · 小学必背", 90),
            AudioTrack("po-003", "悯农", "李绅 · 小学必背", 90),
            AudioTrack("po-004", "登鹳雀楼", "王之涣 · 小学必背", 120),
            AudioTrack("po-005", "咏鹅", "骆宾王 · 启蒙", 90),
            AudioTrack("po-006", "望庐山瀑布", "李白 · 小学必背", 120, premium = true),
            AudioTrack("po-007", "绝句", "杜甫 · 小学必背", 120),
            AudioTrack("po-008", "江雪", "柳宗元 · 小学必背", 90)
        )
    )

    val fmStations: Map<FmGroup, List<FmStation>> = mapOf(
        FmGroup.RECOMMEND to listOf(
            FmStation("fm-cnr-01", "中国之声"),
            FmStation("fm-cnr-02", "经济之声"),
            FmStation("fm-cnr-03", "音乐之声")
        ),
        FmGroup.NATIONAL to listOf(
            FmStation("fm-cnr-01", "中国之声"),
            FmStation("fm-cnr-02", "经济之声"),
            FmStation("fm-cnr-03", "音乐之声"),
            FmStation("fm-cnr-04", "经典音乐广播"),
            FmStation("fm-cnr-05", "环球资讯广播"),
            FmStation("fm-cnr-06", "大湾区之声")
        ),
        FmGroup.LOCAL to listOf(
            FmStation("fm-loc-01", "北京新闻广播"),
            FmStation("fm-loc-02", "上海新闻广播"),
            FmStation("fm-loc-03", "广东新闻广播"),
            FmStation("fm-loc-04", "浙江之声"),
            FmStation("fm-loc-05", "江苏新闻广播"),
            FmStation("fm-loc-06", "四川新闻频率")
        )
    )
}
