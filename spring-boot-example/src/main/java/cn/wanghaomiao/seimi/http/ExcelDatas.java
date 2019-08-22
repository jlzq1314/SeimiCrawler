package cn.wanghaomiao.seimi.http;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.lang.Editor;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jodd.io.FileUtil;
import org.seimicrawler.xpath.JXDocument;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelDatas 类
 *
 * @description: 广州医/九州通/云药库 数据抓取
 * @author: Longlive
 * @since: 2019年03月14日 下午 7:42
 */
public class ExcelDatas {

    static final String dirPath = "D:/ZXSCIT/111";
    static final String excelFile = dirPath + "/aaa-{}.xlsx";
    static final List<String> titleColumns = CollUtil.newArrayList("序号", "药品名称", "通用名称", "规格",
            "包装规格", "单位", "生产企业名称", "剂型",
            "九州通", "广州医", "云药库", "分配");
    // 序号.药名_规格_价格_厂家_编号
    static final String DATA_TEMP = "{}.{}_{}_{}_{}_{}";

    // 云药库
    static final String YYK_COOKIE = "JSESSIONID=4F48F199C882B6AAEF131D6933DC9217; IESESSION=alive; pgv_pvi=1765657600; pgv_si=s7015397376; _qddaz=QD.5mpcrh.vhicw1.jta21c7b; _qdda=3-1.1; _qddab=3-qs4tgl.jta21c7e; _qddamta_800103661=3-0; Hm_lvt_8539cb39d9be36d9c290c19f17437b54=1552653890; tencentSig=6319387648; _qddac=3-2-1.1.qs4tgl.jta21c7e; Hm_lpvt_8539cb39d9be36d9c290c19f17437b54=1552653922";
    static final String YYK_URL_TEMP = "http://www.xty999.com/productlist.ac?keyword={}";
    static final String YYK_SPLIT_STR = "：";

    // 九州通
    static final String JZT_COOKIE = "tj2.si=93f094d2-2b7e-7313-245d-a2d1279c539d; tj2.ui=4b0e5512-a52c-0c6b-32d4-0ad7f6acdba6; Hm_lvt_11000000000000000000000000000001=1552356408; branch_id=FDS; loginedBranchId=FDS; ua_id=231127; ua_type=CUST; ua_platform=1; uuid=231127; loginName=zsscx; loginPwd=cgb66002; search_history=%7B%22keyword%22%3A%22%E6%84%9F%E5%86%92%E7%81%B5%E9%A2%97%E7%B2%92%22%7D; TOKEN=jakSfnGPQnrX7nXpz0R; sessionid=jakSfnGPQnrX7nXpz0R; Hm_lpvt_11000000000000000000000000000001=1552653978";
    static final String JZT_URL_TEMP = "http://fds.yyjzt.com/search/merchandise.htm?keyword={}";
    static final String JZT_DETAIL_URL_TEMP = "http://fds.yyjzt.com/front/merchandise/detail/FDS/{}";

    // 广州医
    static final String GZY_COOKIE = "JSESSIONID=28D69694469060763B4FE045ABAB9979.jvm3";
    // 登录后获取
    static final String GZY_ROLEHASHCODE = "f87ca2e3";
    static final String GZY_URL_TEMP = "https://redirect.gzmpc.com/werp/extjsgridQueryServlet/query";

    public static void main(String[] args) {
        ThreadUtil.newThread(new Runnable() {
            @Override
            public void run() {
                searchData(StrFormatter.format(excelFile, 1));
            }
        }, "thread-1").start();

        ThreadUtil.newThread(new Runnable() {
            @Override
            public void run() {
                searchData(StrFormatter.format(excelFile, 2));
            }
        }, "thread-2").start();

        ThreadUtil.newThread(new Runnable() {
            @Override
            public void run() {
                searchData(StrFormatter.format(excelFile, 3));
            }
        }, "thread-3").start();

        ThreadUtil.newThread(new Runnable() {
            @Override
            public void run() {
                searchData(StrFormatter.format(excelFile, 4));
            }
        }, "thread-4").start();

        ThreadUtil.newThread(new Runnable() {
            @Override
            public void run() {
                searchData(StrFormatter.format(excelFile, 5));
            }
        }, "thread-5").start();
//        GZYData("山莨菪碱注射液");
    }

    /**
     * 主程序，抓取数据
     * @param fileName Excel文件名
     */
    private static void searchData(String filePath) {
        ExcelReader reader = ExcelUtil.getReader(filePath);
        List<Map<String, Object>> filterDatas = CollUtil.filter(reader.readAll(), new Editor<Map<String, Object>>() {
            @Override
            public Map<String, Object> edit(Map<String, Object> data) {
                Dict dDict = new Dict(data);
                String name = dDict.getStr("药品名称");
                dDict.set(titleColumns.get(8), JZTData(name));
                dDict.set(titleColumns.get(9), GZYData(name));
                dDict.set(titleColumns.get(10), YYKData(name));
                dDict.remove("");
                return dDict;
            }
        });
        excelExport(filterDatas, filePath);
    }

    /**
     * 云药库数据抓取
     * @param name 药名称
     */
    private static String YYKData(String name) {
        StringBuilder result = StrUtil.builder();
        if (StrUtil.isNotBlank(name)) {
            HttpRequest request = HttpUtil.createGet(StrFormatter.format(YYK_URL_TEMP, name));
            request.cookie(YYK_COOKIE);
            JXDocument response = JXDocument.create(request.execute().body());
            List<Object> itemList = response.sel("//ul[@class='itemlist']/li[@class='item productItem']/div[@class='m-main']/div[@class='mr']");
            if (CollUtil.isEmpty(itemList)) {
                itemList = CollUtil.newArrayList();
                result.append("无搜索结果");
            }
            for (int i = 0; i < itemList.size(); i++) {
                // 序号.药名_规格_价格_厂家_编号
                JXDocument panelInfo = JXDocument.create(itemList.get(i) + "");
                // 名字（华佗再造丸）
                String mz = Convert.toStr(panelInfo.selOne("//div[@class='mt']/h3/a//text()"), "");
                // 规格（产品规格：8g*12袋/盒）
                String gg = Convert.toStr(panelInfo.selOne("//div[@class='mb']/ul[@class='mb-item01']/li[4]//text()"), "");
                // 厂家（生产厂家：广州白云山奇星药业有限公司）
                String cj = Convert.toStr(panelInfo.selOne("//div[@class='mb']/ul[@class='mb-item01']/li[3]//text()"), "");
                // 编号（商品编号：12942）
                String bh = Convert.toStr(panelInfo.selOne("//div[@class='mb']/ul[@class='mb-item02']/li[4]//text()"), "");
                // 价格（￥33.00）
                String jg = Convert.toStr(panelInfo.selOne("//div[@class='mb']/ul[@class='mb-item04']/li[@class='mi-pri']/span[@class='info']//text()"), "");
                result.append(StrFormatter.format(DATA_TEMP, (i + 1), mz, StrUtil.subAfter(gg, YYK_SPLIT_STR, true), jg,
                        StrUtil.subAfter(cj, YYK_SPLIT_STR, true), StrUtil.subAfter(bh, YYK_SPLIT_STR, true)));
                if (i != (itemList.size() - 1)) {
                    result.append(StrUtil.CRLF);
                }
            }
        }
        return result.toString();
    }

    /**
     * 九州通数据抓取
     * @param name 药名称
     */
    private static String JZTData(String name) {
        StringBuilder result = StrUtil.builder();
        if (StrUtil.isNotBlank(name)) {
            HttpRequest request = HttpUtil.createGet(StrFormatter.format(JZT_URL_TEMP, URLUtil.encode(name, CharsetUtil.UTF_8)));
            request.cookie(JZT_COOKIE);
            JXDocument response = JXDocument.create(request.execute().body());
            List<Object> itemList = response.sel("//div[@id='container_m_search_result_list']/ul/li");
            if (CollUtil.isEmpty(itemList)) {
                itemList = CollUtil.newArrayList();
                result.append("无搜索结果");
            }
            for (int i = 0; i < itemList.size(); i++) {
                // 序号.药名_规格_价格_厂家_编号
                JXDocument panelInfo = JXDocument.create(itemList.get(i) + "");
                // 商品链接
                String gHref = Convert.toStr(panelInfo.selOne("//a[@class='u_img u_goods_img']/@onclick"), "");
                if (StrUtil.isBlank(gHref)) {
                    continue;
                }
                // 商品ID，组装明细链接跳转
                String gId = StrUtil.subBetween(gHref, "/FDS/", StrUtil.SLASH);
                if (StrUtil.isBlank(gId)) {
                    continue;
                }
                // 明细抓取
                result.append(JZTDetailData((i + 1), gId));
                if (i != (itemList.size() - 1)) {
                    result.append(StrUtil.CRLF);
                }
            }
        }
        return result.toString();
    }

    /**
     * 九州通明细信息抓取
     * @param goodId 商品ID
     * @return 序号.药名_规格_价格_厂家_编号
     */
    private static String JZTDetailData(int index, String goodId) {
        HttpRequest requestDetail = HttpUtil.createGet(StrFormatter.format(JZT_DETAIL_URL_TEMP, goodId));
        requestDetail.cookie(JZT_COOKIE);
        JXDocument panelInfo = JXDocument.create(requestDetail.execute().body());
        // 名字（华佗再造丸）
        String mz = Convert.toStr(panelInfo.selOne("//div[@class='g_goods_inner']/div[@class='m_detail_sec'][1]/div[@class='m_sec_bd']/dl[@class='col-lg-4 col-md-4 m_goods_attr'][1]/dd//text()"), "");
        // 规格（120g 浓缩水蜜丸）
        String gg = Convert.toStr(panelInfo.selOne("//div[@class='g_goods_inner']/div[@class='m_detail_sec'][1]/div[@class='m_sec_bd']/dl[@class='col-lg-4 col-md-4 m_goods_attr'][2]/dd//text()"), "");
        // 厂家（广州白云山奇星药业有限公司）
        String cj = Convert.toStr(panelInfo.selOne("//div[@class='g_goods_inner']/div[@class='m_detail_sec'][1]/div[@class='m_sec_bd']/dl[@class='col-lg-8 col-md-8 m_goods_attr']/dd//text()"), "");
        // 编号（CCL008003G）
        String bh = Convert.toStr(panelInfo.selOne("//div[@class='g_goods_inner']/div[@class='m_detail_sec'][1]/div[@class='m_sec_bd']/dl[@class='col-lg-4 col-md-4 m_goods_attr'][8]/dd//text()"), "");
        if ("已认证".equals(bh) || "已合格".equals(bh)) {
            bh = Convert.toStr(panelInfo.selOne("//div[@class='g_goods_inner']/div[@class='m_detail_sec'][1]/div[@class='m_sec_bd']/dl[@class='col-lg-4 col-md-4 m_goods_attr'][7]/dd//text()"), "");
        }
        // 价格（￥33.00）
        String jg = Convert.toStr(panelInfo.selOne("//div[@class='m_goods_info']/div[@class='m_goods_sale m_sale_acti']/div[@class='m_sale_bd']/dl[@class='u_goods_item']/dd/p[@class='m_goods_pric']/span[@class='u_goods_pric u_purc_price']//text()"), "");
        return StrFormatter.format(DATA_TEMP, index, mz, gg, jg, cj, bh);
    }

    /**
     * 广州医数据抓取
     * @param name 药名称
     * @return 序号.药名_规格_价格_厂家_编号
     */
    private static String GZYData(String name) {
        StringBuilder result = StrUtil.builder("无搜索结果");
        if (StrUtil.isNotBlank(name)) {
            HttpRequest request = HttpUtil.createGet(GZY_URL_TEMP);
            request.cookie(GZY_COOKIE);
            request.form(GZYParms(name));
            String responseJson = request.execute().body();
            if (StrUtil.isNotBlank(responseJson)) {
                Dict jsonResult = new Dict(JSONUtil.toBean(responseJson, HashMap.class));
                String rowsStr = jsonResult.getStr("rows");
                if (StrUtil.isNotBlank(rowsStr)) {
                    List<HashMap> rowList = JSONUtil.toList((JSONArray) jsonResult.get("rows"), HashMap.class);
                    if (rowList.size() > 0) {
                        result = StrUtil.builder();
                    }
                    for (int i = 0; i < rowList.size(); i++) {
                        Dict rowDict = new Dict(rowList.get(i));
                        // 名字（注射用维库溴铵）
                        String mz = rowDict.get("goodsformalname", "");
                        // 规格（4mg/支*50支(附50支注射用水)）
                        String gg = rowDict.get("goodstype", "");
                        // 厂家（欧加农公司荷兰）
                        String cj = rowDict.get("factoryname", "");
                        // 编号（H231644）
                        String bh = rowDict.get("goodsopcode", "");
                        // 价格（1425）
                        String jg = rowDict.get("unitprice", "");
                        result.append(StrFormatter.format(DATA_TEMP, (i + 1), mz, gg, jg, cj, bh));
                        if (i != (rowList.size() - 1)) {
                            result.append(StrUtil.CRLF);
                        }
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * 广州医请求参数组装
     * @param name 药品名
     */
    private static Dict GZYParms(String name) {
        return Dict.create()
                .set("value1_1", "%" + name)
                .set("_dc", "1552578627423")
                .set("startIndex", "0")
                .set("pageRowNum", "60")
                .set("needpagecount", "true")
                .set("gridcode", "func-mygoods-grid")
                .set("queryType", "query")
                .set("dataSource", "ScmGoodsNomatchQuery")
                .set("stagetype", "")
                .set("stageid", "")
                .set("rolehashcode", GZY_ROLEHASHCODE)
                .set("querymoduleid", "FuncMygoodsNorQuery")
                .set("sumfieldnames", "")
                .set("orderfields", "")
                .set("fieldName_0", "customid")
                .set("opera_0", "oper_equal")
                .set("value1_0", "110694")
                .set("fieldName_1", "goodsname")
                .set("opera_1", "oper_like")
                .set("oper_length", "2")
                .set("page", "1")
                .set("start", "0")
                .set("limit", "25");
    }

    /**
     * 导出到Excel
     */
    public static void excelExport(List<Map<String, Object>> excelDatas, String oldFileUrl) {
        if (CollUtil.isEmpty(excelDatas)) {
            return;
        }
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + excelDatas.size());
        String now = DateUtil.today();
        // D:/ZXSCIT/111/陈星海医院-1.xlsx
        String oldFileName = StrUtil.subAfter(oldFileUrl, StrUtil.SLASH, true);
        String fileName = StrFormatter.format("{}/{}-new.xlsx", dirPath, StrUtil.subBefore(oldFileName, StrUtil.DOT, true));
        // 判断文件是否存在，存在删除后再写入
        if (FileUtil.isExistingFile(new File(fileName))) {
            try {
                FileUtil.delete(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter(fileName);
        // 设置单元格强制换行
        writer.getStyleSet().getCellStyle().setWrapText(true);

        // 合并单元格后的标题行，使用默认标题样式
        int columnSize = titleColumns.size();
        writer.merge(columnSize - 1, "陈星海" + now);
        for (int i = 0; i < columnSize; i++) {
            writer.autoSizeColumn(i);
        }
        // 云药库
        writer.setColumnWidth((columnSize - 2), 80);
        // 广州医
        writer.setColumnWidth((columnSize - 3), 80);
        // 九州通
        writer.setColumnWidth((columnSize - 4), 80);
        // 生产企业名称
        writer.setColumnWidth((columnSize - 6), 35);
        // 通用名称
        writer.setColumnWidth((columnSize - 10), 15);
        // 药品名称
        writer.setColumnWidth((columnSize - 11), 18);

        // 一次性写出内容，使用默认样式
        writer.write(excelDatas, true);
        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + excelDatas.size());
    }

}
