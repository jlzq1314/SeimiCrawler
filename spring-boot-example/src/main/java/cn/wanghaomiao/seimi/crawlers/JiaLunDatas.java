package cn.wanghaomiao.seimi.crawlers;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.log.StaticLog;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.wanghaomiao.seimi.annotation.Crawler;
import cn.wanghaomiao.seimi.def.BaseSeimiCrawler;
import cn.wanghaomiao.seimi.http.SeimiAgentContentType;
import cn.wanghaomiao.seimi.struct.Request;
import cn.wanghaomiao.seimi.struct.Response;
import org.seimicrawler.xpath.JXDocument;

import java.util.List;

/**
 * JiaLunDatas 类
 *
 * @description: 嘉伦药业数据抓取
 * @author: Longlive
 * @since: 2018年09月13日 8:55 AM
 */
@Crawler(name = "jialun", delay = 1, httpTimeOut = 360000)
public class JiaLunDatas extends BaseSeimiCrawler {

    // 26个字母
    List<String> letter = CollUtil.newArrayList();

    String hostTemp = "http://home.jlgc.com.cn/{}";
    String urlTemp = StrFormatter.format(hostTemp, "allproductlist/{}/0/0/1/{}.html");
    String detailTemp = "//table[@class='product_detail']/tbody/tr[{}]/td[2]/text()";

    List<Dict> lists = CollUtil.newArrayList();

    int totle = 0;
    int dataNums = 0;
    DateTime recordTime = DateUtil.date();

    @Override
    public String[] startUrls() {
        getLetter();
        login();
        cronTime();

        List<String> urls = CollUtil.newArrayList();
        for (String lett : letter) {
            urls.add(StrFormatter.format(urlTemp, lett, 1));
        }
        return ArrayUtil.toArray(urls, String.class);
    }


    @Override
    public void start(Response response) {
        getProductUrls(response);
    }

    /**
     * 用户登录
     * @author Longlive
     * @since 2018年09月14日 10:05 AM
     */
    public void login() {
        Request start = Request.build("http://home.jlgc.com.cn/Login.aspx", JiaLunDatas::loginAfter)
                .useSeimiAgent()
                .setSeimiAgentUseCookie(true)
                .setSeimiAgentContentType(SeimiAgentContentType.HTML)
                .setSeimiAgentScript("$(\"#txtLoginId\").val(\"30283\");$(\"#txtPwd\").val(\"38203\");$(\"#btnLogin\").click();");
        push(start);
    }

    public void loginAfter(Response response) {
        StaticLog.info("Login: {}", response.getUrl());
    }

    /**
     * 循环获取产品集合
     * 截取分页数，组装url集合
     * @author Longlive
     * @since 2018年09月14日 12:12 AM
     */
    public void getProductUrls(Response response) {
        JXDocument doc = response.document();

        //分页
        int pageNo = 1;
        int pageSize = Convert.toInt(doc.selOne("//div[@class='gPage']/span[@class='disabled'][2]/i/text()"), 1);
        //获取URL中的字母
        String reqUrlLett = StrUtil.subBetween(response.getUrl(), "/allproductlist/", "/0/0/");

        for (int i = 0; i < pageSize; i++) {
//            StaticLog.info("Url: {} --- Size: {} --- No: {}", response.getUrl(), pageSize, pageNo);
            Request request = Request.build(StrFormatter.format(urlTemp, reqUrlLett, pageNo), JiaLunDatas::getProDetailUrls);
            push(request);
            pageNo++;
        }
    }

    /**
     * 获取产品明细Url链接集合
     */
    public void getProDetailUrls(Response response) {
        StaticLog.info("DetailUrl: {}", response.getUrl());
        JXDocument doc = response.document();
        for (Object s : doc.sel("//li[@class='info']/p[@class='title']/a/@href")) {
//            StaticLog.info("{}--{}", totle, StrFormatter.format(hostTemp, StrUtil.subAfter(s.toString(), "/", true)));
            Request request = Request.build(StrFormatter.format(hostTemp, StrUtil.subAfter(s.toString(), "/", true)), JiaLunDatas::getProductDetailContent)
                        .useSeimiAgent()
                        .setSeimiAgentUseCookie(true)
                        .setSeimiAgentContentType(SeimiAgentContentType.HTML);
            push(request);
            totle++;
        }
    }

    /**
     * 获取产品明细信息集合
     */
    public void getProductDetailContent(Response response) {
        JXDocument doc = response.document();

        //生产日期/有效期
        String rq = StrUtil.trim(Convert.toStr(doc.selOne("//div[@class='product_info']/dl[@class='clearfix']/dd/div[@class='bt bp clearfix']/p[7]/text()")));
        //库存
        String kc = StrUtil.trim(Convert.toStr(doc.selOne("//div[@class='product_info']/dl[@class='clearfix']/dd/div[@class='bt'][2]/p[@class='clearfix']/text()")));
        //售价
        String sj = Convert.toStr(doc.selOne("//div[@class='product_info']/dl[@class='clearfix']/dd/div[@class='bt'][1]/p/span[@class='price']/text()"));
        //零售价
        String lsj = Convert.toStr(doc.selOne("//div[@class='product_info']/dl[@class='clearfix']/dd/div[@class='bt'][1]/p/span[@class='price price_old']/text()"));

        lists.add(Dict.create()
                .set("货号", doc.selOne(StrFormatter.format(detailTemp, "2")))
                .set("商品名", doc.selOne(StrFormatter.format(detailTemp, "3")))
                .set("规格", doc.selOne(StrFormatter.format(detailTemp, "4")))
                .set("通用名", doc.selOne(StrFormatter.format(detailTemp, "7")))
                .set("生产单位", doc.selOne(StrFormatter.format(detailTemp, "11")))
                .set("批准文号", doc.selOne(StrFormatter.format(detailTemp, "10")))
                .set("生产日期", StrUtil.subBetween(rq, "生产日期：", "有效期："))
                .set("有效期", StrUtil.subAfter(rq, "有效期：", true))
                .set("库存", StrUtil.subAfter(kc, "库存：", true))
                .set("售价", StrUtil.subBetween(sj, "￥", "/"))
                .set("零售价", StrUtil.removeAll(lsj, "￥")));

        //记录时间，方便线程对比
        recordTime = DateUtil.date();
        dataNums++;

        StaticLog.info("1111<<<<<{}--{}--{}>>>>>", dataNums, totle, recordTime.toString());
    }

    /**
     * 26个字母生成
     */
    public void getLetter() {
        for(int i = 1;i<=26;i++){
            letter.add(Convert.toStr((char)(96+i)).toUpperCase());
        }
//        //模拟数据 26
//        letter.add("A");
//        letter.add("B");
//        letter.add("U");
//        letter.add("V");
    }

    public void excelExport() {
        System.out.println("导出初始化>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
        int columnNums = 11;
        String now = DateUtil.today();
        // 通过工具类创建writer
        ExcelWriter writer = ExcelUtil.getWriter("D:/ZXSCIT/嘉伦药商平台" + now + ".xlsx");
        // 合并单元格后的标题行，使用默认标题样式
        writer.merge(columnNums - 1, "嘉伦药商平台" + now);

        // 一次性写出内容，使用默认样式
        writer.write(lists);

        for (int i = 0; i < columnNums; i++) {
            writer.autoSizeColumn(i);
        }

        // 关闭writer，释放内存
        writer.close();
        System.out.println("完成导出>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + lists.size());
    }

    /**
     * 定时任务判断是否需要开始执行导出
     */
    public void cronTime() {
        CronUtil.schedule("*/30 * * * * *", new Task() {
            @Override
            public void execute() {
                DateTime dt = DateUtil.date();
                long bt = DateUtil.between(recordTime, dt, DateUnit.MINUTE);
                if(bt > 5) {
                    CronUtil.stop();
                    //大于5分钟，表示服务器已经读取数据完成
                    excelExport();
                }else {
                    StaticLog.info("定时任务运行：{}", dt.toString());
                }
            }
        });

        // 支持秒级别定时任务
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }
}
