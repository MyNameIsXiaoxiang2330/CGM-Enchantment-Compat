/*
 * CGM Enchantment Addon — 外置台词管理系统
 * Copyright (C) 2026 CGM Enchantment Addon Team
 *
 * 台词从 config/cgmenchant/dialogues.json 加载，
 * 每个附魔有独立的台词池，互不干扰，玩家可自由编辑。
 *
 * JSON 结构：
 *   {
 *     "fellbullet": [
 *       "§c凶弹§7》 §c逃跑是没用的...",
 *       "§c凶弹§7》 §c一发子弹完全不够..."
 *     ],
 *     "fellbullet_piercer": [ ... ]
 *   }
 *
 * 首次运行时自动生成默认 JSON 文件。
 */
package com.example.cgmenchant.handler;

import com.example.cgmenchant.CGMEnchantmentMod;
import com.google.gson.*;
import net.minecraft.entity.player.EntityPlayer;

import java.io.*;
import java.util.*;

public class DialogueManager {

    private static final Map<String, String[]> DIALOGUES = new LinkedHashMap<>();
    private static final Random RANDOM = new Random();
    private static final File CONFIG_DIR = new File("config/cgmenchant");
    private static final File DIALOGUE_FILE = new File(CONFIG_DIR, "dialogues.json");

    /**
     * 初始化台词管理器。在服务器启动时调用。
     * 配置文件存在则加载，否则从默认数据生成。
     */
    public static void init() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }

        if (DIALOGUE_FILE.exists()) {
            loadFromFile();
        } else {
            generateDefaultFile();
        }
    }

    // ===================================================================
    //  JSON 加载
    // ===================================================================

    private static void loadFromFile() {
        try (Reader reader = new FileReader(DIALOGUE_FILE)) {
            JsonObject obj = new JsonParser().parse(reader).getAsJsonObject();
            DIALOGUES.clear();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonArray arr = entry.getValue().getAsJsonArray();
                String[] lines = new String[arr.size()];
                for (int i = 0; i < arr.size(); i++) {
                    lines[i] = arr.get(i).getAsString();
                }
                DIALOGUES.put(entry.getKey(), lines);
            }
            CGMEnchantmentMod.getLogger().info(
                "[DialogueManager] Loaded {} dialogue sets from dialogues.json", DIALOGUES.size());
        } catch (Exception e) {
            CGMEnchantmentMod.getLogger().error("[DialogueManager] Failed to load dialogues.json, using defaults", e);
            loadDefaults();
        }
    }

    // ===================================================================
    //  默认文件生成（首次运行）
    // ===================================================================

    private static void generateDefaultFile() {
        loadDefaults();

        JsonObject obj = new JsonObject();
        for (Map.Entry<String, String[]> entry : DIALOGUES.entrySet()) {
            JsonArray arr = new JsonArray();
            for (String line : entry.getValue()) {
                arr.add(new JsonPrimitive(line));
            }
            obj.add(entry.getKey(), arr);
        }

        try (Writer writer = new FileWriter(DIALOGUE_FILE)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(obj, writer);
            CGMEnchantmentMod.getLogger().info(
                "[DialogueManager] Generated default dialogues.json at {}", DIALOGUE_FILE.getAbsolutePath());
        } catch (IOException e) {
            CGMEnchantmentMod.getLogger().error("[DialogueManager] Failed to write default dialogues.json", e);
        }
    }

    // ===================================================================
    //  默认台词数据（与之前 CommandCGMEnchant 中的一致）
    // ===================================================================

    private static void loadDefaults() {
        DIALOGUES.put("fellbullet", new String[]{
            "§c凶弹§7》 §c逃跑是没用的。四散而去的子弹，不论在哪儿都能华丽地击碎你的脑壳。",
            "§c凶弹§7》 §c一发子弹完全不够……直到枪声和烟雾……把一切记忆埋葬为止……!",
            "§c凶弹§7》 §c我能感知到沉重的枪管压于胃肠之上。让这凶恶的弹丸贯穿他们……!",
            "§c凶弹§7》 §c消失四散的记忆或是对他们的窥视，都没有必要……只需射击，一直射击!",
            "§c凶弹§7》 §c无需为行将就木者惋惜。……若能寻得，终将……那般。",
            "§c凶弹§7》 §c我与我之友人皆为此奋斗，将要为我所贯穿的同僚亦…… ",
            "§c凶弹§7》 §c漆黑的血块将伴随清脆的枪响四散开来。所射出的子弹亦会裂为数股，将他们悉数贯穿。如此一来，同僚们的牺牲便有其价值了。",
            "§c凶弹§7》 §c不……哪怕一开始共为同僚，如今这记忆也早已模糊了！",
            "§c凶弹§7》 §c无论是这枪，抑或是这褴褛……都在时刻向我低语。不要瞄准敌人，而是瞄准那些仍铭记着爱的人。",
            "§c凶弹§7》 §c要将其摧毁，射击，射...?……我刚才说到哪里了？ ",
            "§c凶弹§7》 §c可你们……却依然记得所爱之人，这就是代价",
            "§c凶弹§7》 §c我要把你们的脑袋全部打爆。一个不留。",
            "§c凶弹§7》 §c我已经什么都想不起来了。连其是否存在过都模糊不清。",
            "§c凶弹§7》 §c从我耳边滚开！",
            "§c凶弹§7》 §c这里是战场，而我的枪必将打爆谁。",
            "§c凶弹§7》 §c因此我随意将枪口指向任何人都没问题。",
            "§c凶弹§7》 §c这把霰弹枪无论从哪儿开火，都能像就在面前射击一般，华丽地轰碎呢？",
            "§c凶弹§7》 §c说明你仍然记得，并且还会永远铭记下去吧。",
            "§c凶弹§7》 §c让你的所爱之人记住，你只不过是个脑袋被击飞的受害者。",
            "§c凶弹§7》 §c那就是散落的靶子之一了。",
            "§c凶弹§7》 §c既然如此，得连那脑袋也一起打爆才行。",
            "§c凶弹§7》 §c你瞥见了那个春日。那对我而言也是难得的回忆。 ",
            "§c凶弹§7》 §c不要因过去而做出错误判断。会令我不禁想要破坏一切。",
            "§c凶弹§7》 §c为了追寻大义，些许牺牲在所难免。 ",
            "§c凶弹§7》 §c怀揣着迈向新时代的心。",
            "§c凶弹§7》 §c些许必要的牺牲。",
            "§c凶弹§7》 §c请为了大义而牺牲吧。",
            "§c凶弹§7》 §c前方射击",
            "§c凶弹§7》 §c后方射击",
            "§c凶弹§7》 §c极致的悲剧，唯有自己体会方能真正知其痛苦。",
            "§c凶弹§7》 §c贯穿",
            "§c凶弹§7》 §c开火...无论何时，无论何地！",
            "§c凶弹§7》 §c第二次的回忆。",
            "§c凶弹§7》 §c既是无法避免的终焉，至少以所爱的那双手…",
            "§c凶弹§7》 §c相遇乃走向失去的约定。",
            "§c凶弹§7》 §c失去了什么的世界里面，会开出怎样的花呢？",
            "§c凶弹§7》 §c他们将会指点着这以友人之血点缀的褴褛而嘲弄吧。愚不可及。",
            "§c凶弹§7》 §c即便握起■■，我也终究不愿打开。一位友人或曾这么说过。已太迟了，太迟了……",
            "§c凶弹§7》 §c嗯，如此便已足够。所期的目标已然达成。",
            "§c凶弹§7》 §c归途不会过于喧嚷，故也应将令人舒适。",
            "§c凶弹§7》 §c如此射击贯穿。 ",
            "§c凶弹§7》 §c至终，向无底深渊中沉沦吧。 ",
            "§c凶弹§7》 §c自魔法阵分割撕裂而来的子弹",
            "§c凶弹§7》 §c撕裂回忆的子弹",
            "§c凶弹§7》 §c浸透战友之血的子弹",
            "§c凶弹§7》 §c新时代即将到来。",
            "§c凶弹§7》 §c请为崭新的理想献身吧。"
        });

        DIALOGUES.put("Soleme_lament", new String[]{
            // 第一部分
            "黑色的丧服是为哀悼死者的人准备的。",
            "葬礼上需要的是严肃，不需要那些色彩斑斓的配饰。",
            "用蝴蝶为那些长眠于不毛之地的人们献上哀悼吧。",
            "人死后会去向何方？",
            "那么多踏入这个世界的人都到哪里去了？",
            "如果我们有翅膀的话，真的能离开这里吗？等我们被杀死以后，就能得到翅膀吗？",
            "身负重担的悼念者来到此地救赎众生。",
            "可是现在它如同其他人一样受困于此，锐挫望绝地徘徊着，只剩下空洞的信仰。",
            "它为那些无处可去的人们送着棺材，尽管这口棺材对于安放那些无辜的替罪羔羊们来说远远不够。",
            "蝴蝶毫无意义地拍打着翅膀，它们正等待着永眠。",
            "它们别无选择，只能等待。毕竟，一个世界注定会迎来终末。",
            // 第二部分
            "自己的死亡若能被人铭记乃一大幸事。",
            "这里的人们没有时间去铭记死者，只是麻木地等待下一场死亡。",
            "蝴蝶们一声不响地凝视着敌人。",
            "蝴蝶们幻想着那镜花水月的希望以及锐挫望绝的终末。",
            "巨大的棺材无法取代成百上千座坟墓。",
            "他们看到一长列由白色蝴蝶组成的送葬者。",
            "蝴蝶拍打着翅膀，以一种熟悉却又陌生的方式向我们靠近。",
            "这里寸草不生，所以那些蝴蝶是从哪里来的？",
            "它们别无选择，他们无路可回，只能继续。",
            "如同一场永无止境的葬礼，它们依然平静地哀悼着。",
            "它们最后一次思虑着它们的人生。",
            "在最好的时刻，以最好的面貌死去，乃是令人难以想象的幸福。",
            "实际上，死于此地的他们大多都希望尽可能久得活下去。",
            "有些人认为，死亡意味着全新的开始，然而死后只剩下一片空无。",
            // 第三部分
            "永恒的长眠……愿你安息。",
            "我相信，人死后会化作有着小小翅膀的美妙存在。",
            "请不要再折磨自己了，我会帮你解脱。",
            "带着得来的一切，满怀希望地回归安息之所。",
            "死亡再可怖，会比这现实更可怖吗？死后唯有解脱。",
            "很久以前，人们相信，自己死后会化作有着小小翅膀的美妙存在。",
            "这把枪械，承载着空洞的信仰，只为悼念那无家可归之人……",
            "向那些深陷悲伤的可怜灵魂……致以深切的哀悼。",
            "那些蝴蝶，好冷。",
            "向逝者寄托哀思，向生者致以哀悼。",
            "悲伤的泪水，只是无谓地浸透翅膀。",
            "葬礼只消肃穆，无需招摇。",
            "这具棺柩，承载着空洞的信仰，只为悼念那无家可归之人……",
            "人死后会去往何方？",
            "你承受不了，这些蝴蝶的分量。",
            "连这巨大的棺柩，都不足以抚慰那些无辜的牺牲者。",
            "轻柔地拍动翅膀，静候那终将到来的永眠。",
            "我也曾有过，再也不要目睹死亡的想法。",
            "无力地拍打翅膀，静候那终将到来的结局。",
            "他们做错了什么，我又做错了什么。",
            // 第四部分
            "翅翼垂落的蝴蝶凝结于气息中贫瘠的露水为食。",
            "初终。",
            "带来讣告。",
            "汇成这凄惨炮火的蝴蝶会将你火葬！",
            "此后，则一切荡然无存。",
            "将圆内一点与圆外一点以弧线相接。",
            "而最末！直线将予圆以安息。",
            "可怜而又可悲的孩子们啊……我只能，向他们致以哀悼。",
            "那份信念仅是毫无意义的期望……",
            "放下一切，随后安眠吧。",
            "我予你们以哀悼……",
            "请绽放吧，并成为彼此的归宿。",
            "予逝者以祝贺，予弥留者以庄严哀悼。",
            "蝶自棺中而来。",
            "注视垂死之蝶。",
            "射吧，扣下扳机。",
            "并非是蝴蝶子弹从枪口射出，而是生蝶与亡蝶在枪口所向的坐标处绽放。",
            "身负过重之赠礼。双手紧握无处可去之魂灵。",
            "那么，我们该飞往何方？",
            "我曾在破碎的镜中窥见过垂死之蝶。",
            "虽然我身负哀悼的任务，但却无法令那彷徨之蝶绽放生机，只觉遗憾。",
            "小小的蝴蝶翅膀……能够……飞离……这里吗……"
        });

        // 贯霰形暂时复用相同台词，后续可单独定制
        DIALOGUES.put("fellbullet_piercer", new String[]{
            "§c凶弹-贯霰§7》 §c逃跑是没用的。四散而去的子弹，不论在哪儿都能华丽地击碎你的脑壳。",
            "§c凶弹-贯霰§7》 §c一发子弹完全不够……直到枪声和烟雾……把一切记忆埋葬为止……!",
            "§c凶弹-贯霰§7》 §c我能感知到沉重的枪管压于胃肠之上。让这凶恶的弹丸贯穿他们……!",
            "§c凶弹-贯霰§7》 §c消失四散的记忆或是对他们的窥视，都没有必要……只需射击，一直射击!",
            "§c凶弹-贯霰§7》 §c无需为行将就木者惋惜。……若能寻得，终将……那般。",
            "§c凶弹-贯霰§7》 §c我与我之友人皆为此奋斗，将要为我所贯穿的同僚亦…… ",
            "§c凶弹-贯霰§7》 §c漆黑的血块将伴随清脆的枪响四散开来。所射出的子弹亦会裂为数股，将他们悉数贯穿。如此一来，同僚们的牺牲便有其价值了。",
            "§c凶弹-贯霰§7》 §c不……哪怕一开始共为同僚，如今这记忆也早已模糊了！",
            "§c凶弹-贯霰§7》 §c无论是这枪，抑或是这褴褛……都在时刻向我低语。不要瞄准敌人，而是瞄准那些仍铭记着爱的人。",
            "§c凶弹-贯霰§7》 §c要将其摧毁，射击，射...?……我刚才说到哪里了？ ",
            "§c凶弹-贯霰§7》 §c可你们……却依然记得所爱之人，这就是代价",
            "§c凶弹-贯霰§7》 §c我要把你们的脑袋全部打爆。一个不留。",
            "§c凶弹-贯霰§7》 §c我已经什么都想不起来了。连其是否存在过都模糊不清。",
            "§c凶弹-贯霰§7》 §c从我耳边滚开！",
            "§c凶弹-贯霰§7》 §c这里是战场，而我的枪必将打爆谁。",
            "§c凶弹-贯霰§7》 §c因此我随意将枪口指向任何人都没问题。",
            "§c凶弹-贯霰§7》 §c这把霰弹枪无论从哪儿开火，都能像就在面前射击一般，华丽地轰碎呢？",
            "§c凶弹-贯霰§7》 §c说明你仍然记得，并且还会永远铭记下去吧。",
            "§c凶弹-贯霰§7》 §c让你的所爱之人记住，你只不过是个脑袋被击飞的受害者。",
            "§c凶弹-贯霰§7》 §c那就是散落的靶子之一了。",
            "§c凶弹-贯霰§7》 §c既然如此，得连那脑袋也一起打爆才行。",
            "§c凶弹-贯霰§7》 §c你瞥见了那个春日。那对我而言也是难得的回忆。 ",
            "§c凶弹-贯霰§7》 §c不要因过去而做出错误判断。会令我不禁想要破坏一切。",
            "§c凶弹-贯霰§7》 §c为了追寻大义，些许牺牲在所难免。 ",
            "§c凶弹-贯霰§7》 §c怀揣着迈向新时代的心。",
            "§c凶弹-贯霰§7》 §c些许必要的牺牲。",
            "§c凶弹-贯霰§7》 §c请为了大义而牺牲吧。",
            "§c凶弹-贯霰§7》 §c前方射击",
            "§c凶弹-贯霰§7》 §c后方射击",
            "§c凶弹-贯霰§7》 §c极致的悲剧，唯有自己体会方能真正知其痛苦。",
            "§c凶弹-贯霰§7》 §c贯穿",
            "§c凶弹-贯霰§7》 §c开火...无论何时，无论何地！",
            "§c凶弹-贯霰§7》 §c第二次的回忆。",
            "§c凶弹-贯霰§7》 §c既是无法避免的终焉，至少以所爱的那双手…",
            "§c凶弹-贯霰§7》 §c相遇乃走向失去的约定。",
            "§c凶弹-贯霰§7》 §c失去了什么的世界里面，会开出怎样的花呢？",
            "§c凶弹-贯霰§7》 §c他们将会指点着这以友人之血点缀的褴褛而嘲弄吧。愚不可及。",
            "§c凶弹-贯霰§7》 §c即便握起■■，我也终究不愿打开。一位友人或曾这么说过。已太迟了，太迟了……",
            "§c凶弹-贯霰§7》 §c嗯，如此便已足够。所期的目标已然达成。",
            "§c凶弹-贯霰§7》 §c归途不会过于喧嚷，故也应将令人舒适。",
            "§c凶弹-贯霰§7》 §c如此射击贯穿。 ",
            "§c凶弹-贯霰§7》 §c至终，向无底深渊中沉沦吧。 ",
            "§c凶弹-贯霰§7》 §c自魔法阵分割撕裂而来的子弹",
            "§c凶弹-贯霰§7》 §c撕裂回忆的子弹",
            "§c凶弹-贯霰§7》 §c浸透战友之血的子弹",
            "§c凶弹-贯霰§7》 §c新时代即将到来。",
            "§c凶弹-贯霰§7》 §c请为崭新的理想献身吧。"
        });
    }

    // ===================================================================
    //  公开 API
    // ===================================================================

    /**
     * 获取指定附魔的随机台词。
     *
     * @param key 附魔键名（如 "fellbullet"、"fellbullet_piercer"）
     * @return 完整的台词文本（含颜色和前缀），若无可返回空字符串
     */
    public static String getRandom(String key) {
        String[] lines = DIALOGUES.get(key);
        if (lines != null && lines.length > 0) {
            return lines[RANDOM.nextInt(lines.length)];
        }
        // fallback: 取第一个可用的台词集
        for (String[] fallback : DIALOGUES.values()) {
            if (fallback.length > 0) return fallback[RANDOM.nextInt(fallback.length)];
        }
        return "";
    }

    /**
     * 向玩家发送指定附魔的随机台词。
     * 自动遵守 /cgmen text on/off 设置和 chat/action 显示位置。
     *
     * @param player 目标玩家
     * @param key    附魔键名
     */
    public static void sendRandom(EntityPlayer player, String key) {
        if (player == null) return;
        String line = getRandom(key);
        if (line.isEmpty()) return;
        CommandCGMEnchant.sendDialogue(player, line);
    }

    /**
     * 获取已加载的所有台词键名（供 /cgmen text ReadText 使用）。
     */
    public static Set<String> getKeys() {
        return Collections.unmodifiableSet(DIALOGUES.keySet());
    }

    /** 重新加载配置文件（热加载用，暂不开放） */
    public static void reload() {
        DIALOGUES.clear();
        if (DIALOGUE_FILE.exists()) {
            loadFromFile();
        } else {
            generateDefaultFile();
        }
    }
}
