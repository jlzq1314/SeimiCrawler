package cn.wanghaomiao.seimi.crawlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.seimicrawler.xpath.JXDocument;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * YunYaoKuDatas 类
 *
 * @description: 云药库数据抓取
 * @author: Longlive
 * @since: 2019年03月12日 下午 9:30
 */
@Crawler(name = "yunyaoku", delay = 1, httpTimeOut = 360000, useCookie = true)
public class YunYaoKuDatas extends BaseSeimiCrawler {
    static final String SPLIT_STR = "：";
    static final List<String> ignoreItems = CollUtil.newArrayList("产品单位", "采购数量", "加入进货单");

    String urlTemp = "http://www.xty999.com/productlist.ac?page={}";
    Map<String, String> headers = CollUtil.newHashMap();

    List<Dict> lists = CollUtil.newArrayList();

    // 当前分页数
    int page = 1;
    // 分页 383
    int pageSize = 383;
    // Excel列数
    int excelColumnNums = 1;
    // 整个商城的商品总数，从网页上获取
    int allGoodNums = 9552;

    @Override
    public String[] startUrls() {
        return null;
    }

    @Override
    public List<Request> startRequests() {
        headers.put("Cookie", "JSESSIONID=E4F02FD4A70CF064E9865BCDE6E055DE; IESESSION=alive; pgv_pvi=6726793216; pgv_si=s3235930112; _qddaz=QD.jy2qe9.15p52a.jt5rdapz; Hm_lvt_8539cb39d9be36d9c290c19f17437b54=1552394107; tencentSig=2981355520; ProductHistory_2_301442=301442; ProductHistory_2_26=26; ProductHistory_2_24894=24894; ProductHistory_2_143269=143269; _qdda=3-1.1; _qddab=3-73no2a.jt5ulqtb; ProductHistory_2_127229=127229; _qddamta_800103661=3-0; Hm_lpvt_8539cb39d9be36d9c290c19f17437b54=1552400110");
        List<Request> requests = new LinkedList<>();
        for (int i = 1; i <= pageSize; i++) {
            Request request = Request.build(StrFormatter.format(urlTemp, i), YunYaoKuDatas::start);
            request.setHttpMethod(HttpMethod.GET);
            request.setHeader(headers);
            requests.add(request);
        }
        return requests;
    }

    @Override
    public void start(Response response) {
        JXDocument doc = response.document();
        if (page % 20 == 0) {
            // 每20页睡眠5s
            ThreadUtil.sleep(5000);
        }
        for (Object s : doc.sel("//ul[@class='itemlist']/li[@class='item productItem']/div[@class='m-main']/div[@class='mr']")) {
            // 商品信息封装
            Dict msgDict = Dict.create();
            JXDocument panelInfo = JXDocument.create(s + "");
            String title = Convert.toStr(panelInfo.selOne("//div[@class='mt']/h3/a//text()"), "");
            msgDict.set("药名", title);
            for (Object itemMsg : panelInfo.sel("//div[@class='mb']/ul/li//text()")) {
                String itemStr = Convert.toStr(itemMsg, "");
                if (StrUtil.isNotBlank(itemStr)) {
                    List<String> items = StrUtil.splitTrim(itemStr, SPLIT_STR);
                    if (CollUtil.containsAny(items, ignoreItems)) {
                        continue;
                    }
                    msgDict.set(items.get(0), (items.size() > 1 ? items.get(1) : ""));
                }
            }
            excelColumnNums = msgDict.size();
            lists.add(msgDict);
        }
        System.out.println("当前商品页：" + page + " =============");

        // 开始导出
        if (pageSize == page) {
            excelExport();
        } else {
            page++;
        }
    }

     public void excelExport() {
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
        String now = DateUtil.today();
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter("D:/ZXSCIT/云药库" + now + ".xlsx");
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(excelColumnNums - 1, "云药库" + now);

        // 一次性写出内容，使用默认样式
        writer.write(lists);

        for (int i = 0; i < excelColumnNums; i++) {
            writer.autoSizeColumn(i);
        }

        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
    }

    public static void main(String[] args) {
        String asd = "{\"time\":\"0.168秒\",\"totalrows\":\"2\",\"currentpage\":\"1\",\"isquerylastpage\":\"1\",\"totalpage\":\"1\",\"rows\":[{\"useqty\":\"\",\"lastqty\":\"\",\"approvedocno\":\"国药准字H10970062\",\"factoryname\":\"齐鲁制药\",\"feedbackdate\":\"\",\"prodarea\":\"齐鲁制药有限公司\",\"zdflag\":\"\",\"cgyemployeename\":\"詹钦臻\",\"mygoodsflag\":\"\",\"goodsopcode\":\"B251621\",\"lastsadate\":\"\",\"lsj\":\"368\",\"ssgl\":\"05\",\"fbmemo\":\"\",\"barcode\":\"6915798000900\",\"stageid\":\"1\",\"jhyemployeename\":\"许铭\",\"goodsformalname\":\"盐酸昂丹司琼片\",\"total_line\":\"\",\"packsizein\":\"120件,360件(360)\",\"goodsnoname07\":\"处方药\",\"goodsid\":\"1000001040\",\"stqty\":\"0\",\"factid\":\"218143\",\"packname\":\"件\",\"stockmemo\":\"遮光，密封，在阴凉(超过20度)干燥处保存\",\"platformid\":\"\",\"_validcode\":\"14f5172b\",\"expectdate\":\"\",\"supplyfeedback\":\"\",\"cgyemployeeid\":\"18712\",\"exceptprice\":\"\",\"unitprice\":\"96\",\"bsxflag\":\"0\",\"lastprice\":\"\",\"rownum_\":\"1\",\"goodsnoname13\":\"消化系统用药\",\"goodspinyin\":\"YSADSQP\",\"goodsunit\":\"合\",\"midpacksize\":\"\",\"dtlmemo\":\"\",\"baseyp\":\"0\",\"goodsqty\":\"\",\"goodsname\":\"盐酸昂丹司琼片\",\"packmaterial\":\"铝塑\",\"packsize\":\"1\",\"storagecondition\":\"5\",\"goodstype\":\"4mg12片\",\"jhyemployeeid\":\"16684\",\"pricetype\":\"批优价\",\"stmemo\":\"无货\"},{\"useqty\":\"\",\"lastqty\":\"\",\"approvedocno\":\"国药准字H10970155\",\"factoryname\":\"北大医药\",\"feedbackdate\":\"\",\"prodarea\":\"北大医药\",\"zdflag\":\"\",\"cgyemployeename\":\"詹晓贞\",\"mygoodsflag\":\"\",\"goodsopcode\":\"B251633\",\"lastsadate\":\"\",\"lsj\":\"152\",\"ssgl\":\"05\",\"fbmemo\":\"\",\"barcode\":\"6921184568268\",\"stageid\":\"1\",\"jhyemployeename\":\"詹晓贞\",\"goodsformalname\":\"盐酸昂丹司琼片\",\"total_line\":\"\",\"packsizein\":\"200件,60件(60)\",\"goodsnoname07\":\"处方药\",\"goodsid\":\"1000108170\",\"stqty\":\"3135\",\"factid\":\"219827\",\"packname\":\"件\",\"stockmemo\":\"遮光，密封，在阴凉（不超过20度）干燥处保存\",\"platformid\":\"\",\"_validcode\":\"b69af0b1\",\"expectdate\":\"\",\"supplyfeedback\":\"\",\"cgyemployeeid\":\"17453\",\"exceptprice\":\"\",\"unitprice\":\"130\",\"bsxflag\":\"0\",\"lastprice\":\"\",\"rownum_\":\"2\",\"goodsnoname13\":\"消化系统用药\",\"goodspinyin\":\"YSADSQ\",\"goodsunit\":\"盒\",\"midpacksize\":\"\",\"dtlmemo\":\"\",\"baseyp\":\"0\",\"goodsqty\":\"\",\"goodsname\":\"盐酸昂丹司琼片\",\"packmaterial\":\"铝塑泡罩\",\"packsize\":\"1\",\"storagecondition\":\"5\",\"goodstype\":\"4mg*10片\",\"jhyemployeeid\":\"17453\",\"pricetype\":\"批优价\",\"stmemo\":\"有货\"}]}";
        Dict jsonResult = new Dict(JSONUtil.toBean(asd, HashMap.class));
        String rowsStr = jsonResult.getStr("rows");
        List<HashMap> rowList = JSONUtil.toList((JSONArray) jsonResult.get("rows"), HashMap.class);
        System.out.println("111");
    }
}
