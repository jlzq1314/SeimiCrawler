package cn.wanghaomiao.seimi.crawlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.StaticLog;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.HttpMethod;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.seimicrawler.xpath.JXDocument;

import java.util.List;
import java.util.Map;

/**
 * JiuZhouTongDatas 类
 *
 * @description: 九州通数据抓取（明细页）
 * @author: Longlive
 * @since: 2019年03月13日 16:35
 */
@Crawler(name = "jiuzhoutong", useCookie = true, httpTimeOut = 3600000)
public class JiuZhouTongDatas extends BaseSeimiCrawler {
    static final String SPLIT_STR = "：";

    String urlTemp = "http://fds.yyjzt.com/search/merchandise.htm?page={}";
    String detailTemp = "http://fds.yyjzt.com/front/merchandise/detail/FDS/{}";

    Map<String, String> headers = CollUtil.newHashMap();
    List<Dict> lists = CollUtil.newArrayList();

    // 分页
    int pageSize = 1;
    // Excel列数
    int excelColumnNums = 1;
    // 当前运行时间
    DateTime recordTime = DateUtil.date();
    boolean isFirst = true;

    @Override
    public String[] startUrls() {
        return null;
    }

    @Override
    public List<Request> startRequests() {
        if (isFirst) {
            // 开始计时
            cronTime();
            isFirst = false;
        }

        List<Request> requests = CollUtil.newArrayList();
        headers.put("Cookie", "tj2.si=93f094d2-2b7e-7313-245d-a2d1279c539d; tj2.ui=4b0e5512-a52c-0c6b-32d4-0ad7f6acdba6; Hm_lvt_11000000000000000000000000000001=1552356408; branch_id=FDS; TOKEN=4R2avTAEgF0q36hic1G; loginedBranchId=FDS; ua_id=231127; ua_type=CUST; ua_platform=1; sessionid=4R2avTAEgF0q36hic1G; uuid=231127; loginName=zsscx; loginPwd=cgb66002; search_history=%7B%22keyword%22%3A%22%E6%84%9F%E5%86%92%E7%81%B5%E9%A2%97%E7%B2%92%22%7D; Hm_lpvt_11000000000000000000000000000001=1552493362");
        headers.put("Content-Type", "text/html;charset=UTF-8");

        for (int i = 1; i <= pageSize; i++) {
            Request request = Request.build(StrFormatter.format(urlTemp, i), JiuZhouTongDatas::start)
                    .setHttpMethod(HttpMethod.GET)
                    .setHeader(headers);
            requests.add(request);
        }

        return requests;
    }

    @Override
    public void start(Response response) {
        System.out.println("URL：" + response.getUrl());

        JXDocument doc = response.document();
        List<Object> liLists = doc.sel("//div[@id='container_m_search_result_list']/ul/li");
        if (CollUtil.isNotEmpty(liLists)) {
            for (Object liObj : liLists) {
                JXDocument liDoc = JXDocument.create(liObj + "");

                // 商品属性，排除 "已售罄" 的情况
                String gStatus = Convert.toStr(liDoc.selOne("//span[@class='u_goods_buyed']/text()"), "");
                if ("已售罄".equals(gStatus)) {
                    continue;
                }

                // 商品链接
                String gHref = Convert.toStr(liDoc.selOne("//a[@class='u_img u_goods_img']/@onclick"), "");
                if (StrUtil.isBlank(gHref)) {
                    continue;
                }
                // 商品ID，组装明细链接跳转
                String gId = StrUtil.subBetween(gHref, "/FDS/", StrUtil.SLASH);
                if (StrUtil.isBlank(gId)) {
                    continue;
                }

                // 商品属性信息
                Dict msgDict = Dict.create();
                for (Object tdMsg : liDoc.sel("//div[@class='m_goods_deta_inner']/table[@class='table m_goods_tb']/tbody/tr/td[position()>1]//text()")) {
                    String tdStr = Convert.toStr(tdMsg, "");
                    if (StrUtil.isNotBlank(tdStr)) {
                        List<String> items = StrUtil.splitTrim(tdStr, SPLIT_STR);
                        if (items.size() > 1) {
                            msgDict.set(items.get(0), items.get(1));
                        } else {
                            msgDict.set("采购价", items.get(0));
                        }
                    }
                }

                // 组装商品明细Url,并且进行跳转
                Request request = Request.build(StrFormatter.format(detailTemp, gId), JiuZhouTongDatas::getProductDetail)
                        .setHttpMethod(HttpMethod.GET)
                        .setHeader(headers)
                        .setMeta(msgDict);
                push(request);
            }
        }
        // 记录当前操作时间
        recordTime = DateUtil.date();
    }

    /**
     * 获取商品详情信息
     * @param response
     */
    public void getProductDetail(Response response) {
        System.out.println("URL：" + response.getUrl() + " === " + lists.size());

        // 商品面版信息
        Dict msgDict = Dict.create();

        JXDocument doc = response.document();
        for (Object itemMsg : doc.sel("//div[@class='g_goods_inner']/div[@class='m_detail_sec']/div[@class='m_sec_bd']/dl//text()")) {
            String itemStr = Convert.toStr(itemMsg, "");
            if (StrUtil.isNotBlank(itemStr)) {
                List<String> items = StrUtil.splitTrim(itemStr, SPLIT_STR);
                if (items.contains("产地")) {
                    // 忽略 "产地" 字段
                    continue;
                }
                msgDict.put(items.get(0), (items.size() > 1 ? items.get(1) : ""));
            }
        }

        // 加入Request传过来的信息
        msgDict.putAll(response.getMeta());

        excelColumnNums = msgDict.size();
        lists.add(msgDict);

        // 记录当前操作时间
        recordTime = DateUtil.date();
//            System.out.println("商品数：" + excelColumnNums + " ==== ");
    }

    /**
     * 定时任务判断是否需要开始执行导出
     */
    public void cronTime() {
        System.out.println("数据抓取开始，时间：" + recordTime.toString());
        // 每20秒运行一次
        CronUtil.schedule("0/20 * * * * * ", new Task() {
            @Override
            public void execute() {
                DateTime dt = DateUtil.date();
                long bt = DateUtil.between(recordTime, dt, DateUnit.MINUTE);
                if (bt > 0) {
                    CronUtil.stop();
                    //大于30分钟，表示服务器已经读取数据完成
                    excelExport();
                    System.out.println("数据抓取结束，时间：" + dt + "，相隔：" + bt + "分钟");
                } else {
                    StaticLog.info("定时任务，原时间：{} ===== 当前时间：{} ===== 相差：{}分钟 ===== 商品数量：{}",
                            recordTime.toString(), dt.toString(), bt, lists.size());
                }
            }
        });

        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    public void excelExport() {
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
        String now = DateUtil.today();
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter("D:/ZXSCIT/九州通" + now + ".xlsx");
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(excelColumnNums - 1, "九州通" + now);

        // 一次性写出内容，使用默认样式
        writer.write(lists);

        for (int i = 0; i < excelColumnNums; i++) {
            writer.autoSizeColumn(i);
        }

        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
    }

}
